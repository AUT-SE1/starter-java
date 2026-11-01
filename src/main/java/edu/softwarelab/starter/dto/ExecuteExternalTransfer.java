package edu.softwarelab.starter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExecuteExternalTransfer {

    @JsonProperty("message_id")
    private UUID messageId;

    @JsonProperty("schema_version")
    private String schemaVersion = "3.0";

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("causation_id")
    private UUID causationId;

    @JsonProperty("occurred_at")
    private OffsetDateTime occurredAt;

    @JsonProperty("producer")
    private String producer = "S3-PAY";

    @JsonProperty("message_type")
    private String messageType = "ExecuteExternalTransfer.v3";

    @JsonProperty("operation_id")
    private UUID operationId;

    @JsonProperty("data")
    private Data data;

    // Getters and Setters...

    public static class Data {
        @JsonProperty("dest_iban")
        private String destIban;

        @JsonProperty("amount")
        private Long amount;

        @JsonProperty("currency")
        private String currency = "IRR";

        // Getters and Setters...
    }
}