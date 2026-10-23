package edu.softwarelab.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests: routing, JSON encoding and the store, working together.
 *
 * <p>The application is started on a random port and driven over real HTTP with
 * the JDK's own client, so these tests depend only on {@code @SpringBootTest}
 * and not on any of the MockMvc test-autoconfiguration packages.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private OrderStore store;

    @BeforeEach
    void startFromAnEmptyStore() {
        // No test inherits another test's state, so the order they run in
        // does not matter.
        store.clear();
    }

    private record Call(int status, String contentType, JsonNode body) {
    }

    private Call send(String method, String path, String jsonBody) throws Exception {
        HttpRequest.BodyPublisher payload = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8);

        HttpRequest.Builder request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, payload);
        if (jsonBody != null) {
            request.header("Content-Type", "application/json");
        }

        HttpResponse<String> response = CLIENT.send(
                request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Call(
                response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(""),
                JSON.readTree(response.body()));
    }

    @Test
    void postOrders_withAValidBody_returns201AndTheOrder() throws Exception {
        Call call = send("POST", "/orders", "{\"item\":\"notebook\",\"quantity\":3}");

        assertEquals(201, call.status());
        assertTrue(call.contentType().startsWith("application/json"),
                () -> "expected a JSON content type, got " + call.contentType());
        assertEquals("notebook", call.body().path("item").asText());
        assertEquals(3, call.body().path("quantity").asInt());
        assertEquals("created", call.body().path("status").asText());
        assertTrue(call.body().path("id").asText().startsWith("ord_"));
    }

    @Test
    void postOrders_withAnInvalidQuantity_returns400() throws Exception {
        Call call = send("POST", "/orders", "{\"item\":\"notebook\",\"quantity\":0}");

        assertEquals(400, call.status());
        assertEquals("validation_failed", call.body().path("error").asText());
        assertEquals("quantity", call.body().path("field").asText());
    }

    @Test
    void postOrders_withMalformedJson_returns400() throws Exception {
        Call call = send("POST", "/orders", "{\"item\":");

        assertEquals(400, call.status());
        assertEquals("validation_failed", call.body().path("error").asText());
    }

    @Test
    void getOrder_afterCreating_returnsTheSameOrder() throws Exception {
        Call created = send("POST", "/orders", "{\"item\":\"کتاب\",\"quantity\":7}");
        String id = created.body().path("id").asText();
        assertFalse(id.isEmpty(), "the create response carried no id");

        Call call = send("GET", "/orders/" + id, null);

        assertEquals(200, call.status());
        assertEquals(id, call.body().path("id").asText());
        assertEquals("کتاب", call.body().path("item").asText());
        assertEquals(7, call.body().path("quantity").asInt());
    }

    @Test
    void getOrder_whenIdIsUnknown_returns404() throws Exception {
        Call call = send("GET", "/orders/ord_missing", null);

        assertEquals(404, call.status());
        assertEquals("not_found", call.body().path("error").asText());
        assertFalse(call.body().has("field"),
                "the contract leaves field out when it is null");
    }

    @Test
    void health_returns200AndStatusOk() throws Exception {
        Call call = send("GET", "/health", null);

        assertEquals(200, call.status());
        assertEquals("ok", call.body().path("status").asText());
    }
}
