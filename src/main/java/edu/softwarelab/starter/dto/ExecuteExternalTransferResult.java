package edu.softwarelab.starter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExecuteExternalTransferResult {

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
    private String producer = "S5-STL";

    @JsonProperty("message_type")
    private String messageType = "ExecuteExternalTransferResult.v3";

    @JsonProperty("operation_id")
    private UUID operationId;

    @JsonProperty("outcome")
    private String outcome; // APPLIED, REJECTED, UNKNOWN, PENDING

    @JsonProperty("data")
    private ResultData data;

    @JsonProperty("error")
    private OperationError error;

    // Getters and Setters...

    public static class ResultData {
        @JsonProperty("dest_iban")
        private String destIban;

        @JsonProperty("amount")
        private Long amount;

        @JsonProperty("currency")
        private String currency = "IRR";

        @JsonProperty("external_reference")
        private String externalReference;

        @JsonProperty("status")
        private String status; // ACCEPTED, PROCESSING, SETTLED

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        // Getters and Setters...
    }

    public static class OperationError {
        @JsonProperty("code")
        private String code;

        @JsonProperty("retryable")
        private Boolean retryable;

        @JsonProperty("public_message")
        private String publicMessage;

        // Getters and Setters...
    }
}