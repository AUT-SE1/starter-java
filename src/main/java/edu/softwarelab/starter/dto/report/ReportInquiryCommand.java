package edu.softwarelab.starter.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportInquiryCommand(
    @JsonProperty("message_id") UUID messageId,
    @JsonProperty("correlation_id") UUID correlationId,
    @JsonProperty("occurred_at") OffsetDateTime occurredAt,
    @JsonProperty("producer") String producer,
    @JsonProperty("message_type") String messageType,
    @JsonProperty("data") InquiryData data
) {
    public record InquiryData(
        @JsonProperty("business_date") LocalDate businessDate,
        @JsonProperty("report_id") UUID reportId
    ) {}
}