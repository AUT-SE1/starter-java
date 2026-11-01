package edu.softwarelab.starter.dto;

public record SettlementReportSummary(
        long totalCount,
        long totalAmount,
        long successCount,
        long failedCount
) {}