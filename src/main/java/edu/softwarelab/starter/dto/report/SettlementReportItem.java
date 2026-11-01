package edu.softwarelab.starter.dto.report;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementReportItem(
        UUID transactionId,
        String correlationId,
        String idempotencyKey,
        String sourceIban,
        String destinationIban,
        Long amount,
        String currency,
        String settlementType, // PAYA یا SATNA
        String status,         // SUCCESS یا FAILED یا PENDING
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime settledAt
) {}