package edu.softwarelab.starter.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportResultEvent(
    @JsonProperty("message_id") UUID messageId,
    @JsonProperty("correlation_id") UUID correlationId,
    @JsonProperty("causation_id") UUID causationId,
    @JsonProperty("producer") String producer,
    @JsonProperty("message_type") String messageType,
    @JsonProperty("data") ReportResultData data
) {
    public record ReportResultData(
        @JsonProperty("report_id") UUID reportId,
        @JsonProperty("business_date") LocalDate businessDate,
        @JsonProperty("outcome") String outcome, // ACCEPTED یا REJECTED
        @JsonProperty("processed_at") OffsetDateTime processedAt,
        @JsonProperty("total_records_processed") Long totalRecordsProcessed,
        @JsonProperty("error") ReportError error
    ) {}

    public record ReportError(
        @JsonProperty("code") String code,
        @JsonProperty("detail") String detail
    ) {}
}