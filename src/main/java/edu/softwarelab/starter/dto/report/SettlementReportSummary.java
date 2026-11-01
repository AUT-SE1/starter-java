package edu.softwarelab.starter.dto.report;

public record SettlementReportSummary(
        long totalCount,
        long totalAmount,
        long successCount,
        long failedCount
) {}