package edu.softwarelab.starter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests: routing, JSON encoding and the store, working together. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderStore store;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void startFromAnEmptyStore() {
        // No test inherits another test's state, so the order they run in
        // does not matter.
        store.clear();
    }

    @Test
    void postOrders_withAValidBody_returns201AndTheOrder() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"notebook\",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.item", is("notebook")))
                .andExpect(jsonPath("$.quantity", is(3)))
                .andExpect(jsonPath("$.status", is("created")))
                .andExpect(jsonPath("$.id", startsWith("ord_")));
    }

    @Test
    void postOrders_withAnInvalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"notebook\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.field", is("quantity")));
    }

    @Test
    void postOrders_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_failed")));
    }

    @Test
    void getOrder_afterCreating_returnsTheSameOrder() throws Exception {
        String body = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"کتاب\",\"quantity\":7}".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode created = json.readTree(body);

        mockMvc.perform(get("/orders/" + created.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.get("id").asText())))
                .andExpect(jsonPath("$.item", is("کتاب")))
                .andExpect(jsonPath("$.quantity", is(7)));
    }

    @Test
    void getOrder_whenIdIsUnknown_returns404() throws Exception {
        mockMvc.perform(get("/orders/ord_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    @Test
    void health_returns200AndStatusOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$", notNullValue()));
    }
}
