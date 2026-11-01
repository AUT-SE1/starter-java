package edu.softwarelab.starter.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SettlementDailyReportEvent(
        @JsonProperty("message_id") UUID messageId,
        @JsonProperty("correlation_id") UUID correlationId,
        @JsonProperty("causation_id") UUID causationId,
        @JsonProperty("producer") String producer,
        @JsonProperty("message_type") String messageType,
        @JsonProperty("data") ReportData data
) {
    public record ReportData(
            @JsonProperty("report_id") UUID reportId,
            @JsonProperty("business_date") LocalDate businessDate,
            @JsonProperty("generated_at") OffsetDateTime generatedAt,
            @JsonProperty("summary") SettlementReportSummary summary,
            @JsonProperty("transactions") List<SettlementReportItem> transactions
    ) {}
}