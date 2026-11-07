package edu.softwarelab.starter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/** نتیجه‌ی S5 → S3: ExecuteExternalTransferResult.v3 */
public record ExecuteExternalTransferResult(
        @JsonProperty("message_id") UUID messageId,
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("causation_id") UUID causationId,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt,
        @JsonProperty("producer") String producer,
        @JsonProperty("message_type") String messageType,
        @JsonProperty("operation_id") UUID operationId,
        @JsonProperty("outcome") String outcome,   // APPLIED, REJECTED, UNKNOWN, PENDING
        @JsonProperty("data") ResultData data,
        @JsonProperty("error") OperationError error
) {
    public record ResultData(
            @JsonProperty("dest_iban") String destIban,
            @JsonProperty("amount") Long amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("status") String status,  // ACCEPTED, PROCESSING, SETTLED
            @JsonProperty("updated_at") OffsetDateTime updatedAt
    ) {
    }

    public record OperationError(
            @JsonProperty("code") String code,
            @JsonProperty("retryable") Boolean retryable,
            @JsonProperty("public_message") String publicMessage
    ) {
    }
}
