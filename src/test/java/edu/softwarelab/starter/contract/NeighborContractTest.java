package edu.softwarelab.starter.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Session 05 — contract test against the NEIGHBOURING TEAM's running sandbox.
 *
 * <p>This is not a mock. It sends real HTTP to a service you did not write and
 * checks the answer against the OpenAPI document both teams agreed on.
 *
 * <p>Run it:
 * <pre>NEIGHBOR_BASE_URL=http://10.0.0.42:8080 make contract-test</pre>
 *
 * <p>With no NEIGHBOR_BASE_URL set, every test here is skipped, so {@code make
 * test} stays green on a laptop with no neighbour in reach. A skipped contract
 * test is not a passing one — your report must show it running against a real
 * address.
 *
 * <p>TODO(team): two things to change, and nothing else.
 * <ol>
 *   <li>NEIGHBOR_ORDERS_PATH, if your neighbour's service names its routes
 *       differently from the shared contract.</li>
 *   <li>validOrderBody(), if their CreateOrderRequest carries extra fields.</li>
 * </ol>
 */
class NeighborContractTest {

    /** TODO(team): confirm against their OpenAPI. */
    private static final String NEIGHBOR_ORDERS_PATH = "/orders";

    private static final Path SPEC_PATH = Path.of("openapi.json");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();

    private static OpenApiCheck document;

    /** TODO(team): match your neighbour's CreateOrderRequest schema. */
    private static Map<String, Object> validOrderBody() {
        return Map.of("item", "contract-test-item", "quantity", 2);
    }

    @BeforeAll
    static void loadTheAgreedDocument() throws IOException {
        document = OpenApiCheck.load(SPEC_PATH);
    }

    private static String baseUrl() {
        String url = System.getenv("NEIGHBOR_BASE_URL");
        assumeTrue(url != null && !url.isBlank(),
                "NEIGHBOR_BASE_URL is not set — skipping the contract test. "
                        + "Set it to your neighbour's sandbox, e.g. http://10.0.0.42:8080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record Call(int status, JsonNode body) {
    }

    private static Call send(String method, String url, Object jsonBody) {
        try {
            HttpRequest.BodyPublisher payload = jsonBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(
                            JSON.writeValueAsString(jsonBody), StandardCharsets.UTF_8);

            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .method(method, payload);
            if (jsonBody != null) {
                request.header("Content-Type", "application/json");
            }

            HttpResponse<String> response =
                    CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // A timestamp belongs in the report, so print one with every call.
            System.out.printf("%s  %s %s -> %d%n",
                    ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    method, url, response.statusCode());

            return new Call(response.statusCode(), JSON.readTree(response.body()));
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AssertionError(method + " " + url
                    + " failed to reach the neighbour's sandbox: " + error
                    + "\nIs their service running, listening on 0.0.0.0 and allowed through the firewall?",
                    error);
        }
    }

    private static void assertMatches(String schemaName, JsonNode value) {
        List<String> problems = document.validate(schemaName, value);
        assertTrue(problems.isEmpty(),
                () -> "response does not match the agreed " + schemaName + " schema:\n  "
                        + String.join("\n  ", problems));
    }

    // --- the contract -------------------------------------------------------

    @Test
    void neighborCreateOrder_returns201AndAnOrderMatchingTheContract() {
        Call call = send("POST", baseUrl() + NEIGHBOR_ORDERS_PATH, validOrderBody());

        assertEquals(201, call.status(), () -> "body: " + call.body());
        assertMatches("Order", call.body());
    }

    @Test
    void neighborCreateOrder_withQuantityZero_returns400AndAnError() {
        Call call = send("POST", baseUrl() + NEIGHBOR_ORDERS_PATH,
                Map.of("item", "contract-test-item", "quantity", 0));

        // The contract fixes the status code and the error shape. It says
        // nothing about the wording of the message — that is their business.
        assertEquals(400, call.status(),
                () -> "expected 400 for an invalid quantity — body: " + call.body());
        assertMatches("Error", call.body());
    }

    @Test
    void neighborGetOrder_afterCreating_returnsTheSameOrder() {
        Call created = send("POST", baseUrl() + NEIGHBOR_ORDERS_PATH, validOrderBody());
        String id = created.body().path("id").asText("");
        assertFalse(id.isEmpty(), () -> "the create response carried no usable id: " + created.body());

        Call call = send("GET", baseUrl() + NEIGHBOR_ORDERS_PATH + "/" + id, null);

        assertEquals(200, call.status(), () -> "reading back order " + id);
        assertMatches("Order", call.body());
        assertEquals(id, call.body().path("id").asText());
    }

    @Test
    void neighborGetOrder_withAnUnknownId_returns404() {
        Call call = send("GET",
                baseUrl() + NEIGHBOR_ORDERS_PATH + "/ord_definitely_not_real", null);

        assertEquals(404, call.status(),
                () -> "expected 404 for an unknown id — body: " + call.body());
        assertMatches("Error", call.body());
    }
}
