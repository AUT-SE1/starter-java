package edu.softwarelab.starter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/** دستور S3 → S5: ExecuteExternalTransfer.v3 */
public record ExecuteExternalTransfer(
        @JsonProperty("message_id") UUID messageId,
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("causation_id") UUID causationId,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt,
        @JsonProperty("producer") String producer,
        @JsonProperty("message_type") String messageType,
        @JsonProperty("operation_id") UUID operationId,
        @JsonProperty("data") Data data
) {
    public record Data(
            @JsonProperty("dest_iban") String destIban,
            @JsonProperty("amount") Long amount,
            @JsonProperty("currency") String currency
    ) {
    }
}
