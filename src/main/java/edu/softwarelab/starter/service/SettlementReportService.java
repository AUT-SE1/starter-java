package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.report.ReportResultEvent;
import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import edu.softwarelab.starter.dto.report.SettlementReportItem;
import edu.softwarelab.starter.dto.report.SettlementReportSummary;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SettlementReportService {

    private static final ZoneId TEHRAN_ZONE = ZoneId.of("Asia/Tehran");
    private static final Set<String> FINALIZED_STATUSES = Set.of("SUCCESS", "FAILED");
    private static final Logger log = LoggerFactory.getLogger(SettlementReportService.class);

    private final SettlementTransferRepository transferRepository;

    public SettlementReportService(SettlementTransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    /**
     * متد مخصوص اجرای خودکار (کرون‌جاب نیمه‌شب)
     */
    @Transactional(readOnly = true)
    public SettlementDailyReportEvent generateDailyReport(LocalDate businessDate) {
        return generateDailyReport(businessDate, UUID.randomUUID(), null);
    }

    /**
     * متد اصلی تولید گزارش (قابل استفاده هم برای کرون‌جاب و هم برای پاسخ به استعلام S6)
     *
     * @param businessDate  تاریخ روز کاری مورد نظر
     * @param correlationId شناسه ردیابی لاگ‌ها (در صورت null بودن، یک مقدار جدید ساخته می‌شود)
     * @param causationId   شناسه پیام استعلام درخواست‌کننده (در حالت کرون null است)
     * @return ساختار نهایی گزارش آماده ارسال
     */
    @Transactional(readOnly = true)
    public SettlementDailyReportEvent generateDailyReport(LocalDate businessDate, UUID correlationId, UUID causationId) {
        log.info("Generating daily report: business_date={}, correlation_id={}, causation_id={}",
                businessDate, correlationId, causationId);

        OffsetDateTime fromDate = businessDate.atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();
        OffsetDateTime toDate = businessDate.plusDays(1).atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();

        List<SettlementTransfer> transfers = transferRepository.findFinalizedTransfersForReport(
                fromDate,
                toDate,
                FINALIZED_STATUSES
        );

        List<SettlementReportItem> items = transfers.stream()
                .map(this::mapToReportItem)
                .toList();

        SettlementReportSummary summary = calculateSummary(items);
        UUID reportId = generateDeterministicReportId(businessDate);

        log.info("Daily report generated: report_id={}, total_records={}, total_amount={}",
                reportId, summary.totalCount(), summary.totalAmount());

        SettlementDailyReportEvent.ReportData reportData = new SettlementDailyReportEvent.ReportData(
                reportId,
                businessDate,
                OffsetDateTime.now(),
                summary,
                items
        );

        return new SettlementDailyReportEvent(
                UUID.randomUUID(),
                correlationId != null ? correlationId : UUID.randomUUID(),
                causationId,
                "S5-STL",
                "SettlementDailyReport.v1",
                reportData
        );
    }


    @Transactional
    public void processReportResult(ReportResultEvent event) {
        if (event == null || event.data() == null) {
            log.warn("Skipping null or empty ReportResultEvent.");
            return;
        }

        var data = event.data();
        UUID reportId = data.reportId();
        LocalDate businessDate = data.businessDate();
        String outcome = data.outcome();

        log.info("Processing report result: report_id={}, business_date={}, outcome={}",
                reportId, businessDate, outcome);

        if ("ACCEPTED".equalsIgnoreCase(outcome)) {
            OffsetDateTime fromDate = businessDate.atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();
            OffsetDateTime toDate = businessDate.plusDays(1).atStartOfDay(TEHRAN_ZONE).toOffsetDateTime();

            int updatedCount = transferRepository.updateTransfersStatusForBusinessDate(
                    fromDate,
                    toDate,
                    "SUCCESS",
                    "SETTLED_CONFIRMED"
            );

            log.info("Successfully updated {} transfers to SETTLED_CONFIRMED for business_date={}",
                    updatedCount, businessDate);

        } else if ("REJECTED".equalsIgnoreCase(outcome)) {
            String code = data.error() != null ? data.error().code() : "UNKNOWN";
            String detail = data.error() != null ? data.error().detail() : "No details provided";

            log.error("Report REJECTED by S6! report_id={}, business_date={}, error_code={}, detail={}",
                    reportId, businessDate, code, detail);
        } else {
            log.warn("Received unrecognized report outcome: {}", outcome);
        }
    }

    private SettlementReportItem mapToReportItem(SettlementTransfer transfer) {
        return new SettlementReportItem(
                transfer.getId(),
                transfer.getCorrelationId(),
                transfer.getIdempotencyKey(),
                transfer.getSourceIban(),
                transfer.getDestinationIban(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getSettlementType(),
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getSettledAt()
        );
    }

    private SettlementReportSummary calculateSummary(List<SettlementReportItem> items) {
        if (items.isEmpty()) {
            return new SettlementReportSummary(0, 0, 0, 0);
        }

        long totalCount = items.size();
        long totalAmount = 0L;
        long successCount = 0L;
        long failedCount = 0L;

        for (SettlementReportItem item : items) {
            if ("SUCCESS".equalsIgnoreCase(item.status())) {
                successCount++;
                totalAmount += (item.amount() != null ? item.amount() : 0L);
            } else if ("FAILED".equalsIgnoreCase(item.status())) {
                failedCount++;
            }
        }

        return new SettlementReportSummary(totalCount, totalAmount, successCount, failedCount);
    }

    private UUID generateDeterministicReportId(LocalDate businessDate) {
        String seed = "SETTLEMENT-DAILY-REPORT:" + businessDate.toString();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}