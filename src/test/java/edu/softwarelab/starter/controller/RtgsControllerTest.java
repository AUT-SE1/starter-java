package edu.softwarelab.starter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RtgsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testProcessRtgs_SuccessPath() throws Exception {
        // مسیر موفق: مبلغ و شبای معتبر
        String validPayload = """
                {
                  "transactionId": "tx-2001",
                  "sourceIban": "IR120000000000000000000001",
                  "destinationIban": "IR120000000000000000000002",
                  "amount": 50000,
                  "description": "Valid transfer"
                }
                """;

        mockMvc.perform(post("/api/v1/rtgs/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    public void testProcessRtgs_ErrorPath_ProblemDetails() throws Exception {
        // مسیر خطا: مبلغ کمتر از حد مجاز
        String invalidPayload = """
                {
                  "transactionId": "tx-2002",
                  "sourceIban": "IR120000000000000000000001",
                  "destinationIban": "IR120000000000000000000002",
                  "amount": 5000,
                  "description": "Invalid amount"
                }
                """;

        // بررسی اینکه کد 400 برگردد و ساختار خطا Problem Details باشد
        mockMvc.perform(post("/api/v1/rtgs/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").isNotEmpty());
    }
}