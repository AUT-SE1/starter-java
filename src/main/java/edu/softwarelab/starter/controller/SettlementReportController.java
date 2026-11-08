package edu.softwarelab.starter.controller;

import edu.softwarelab.starter.dto.SettlementDailyReportEvent;
import edu.softwarelab.starter.service.SettlementReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlement-reports")
public class SettlementReportController {

    private static final Logger log = LoggerFactory.getLogger(SettlementReportController.class);

    private final SettlementReportService reportService;

    public SettlementReportController(SettlementReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * اندپوینت دریافت گزارش تسویه‌های یک روز بر اساس تاریخ
     * GET /api/v1/settlement-reports?date=2026-10-31
     */
    @GetMapping
    public ResponseEntity<SettlementDailyReportEvent.ReportData> getSettlementReport(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // در صورت عدم ارسال تاریخ، روز کاری قبل محاسبه می‌شود
        LocalDate targetDate = (date != null) ? date : LocalDate.now().minusDays(1);

        log.info("REST request received for settlement report: date={}", targetDate);

        SettlementDailyReportEvent reportEvent = reportService.generateDailyReport(
                targetDate,
                UUID.randomUUID(),
                null
        );

        return ResponseEntity.ok(reportEvent.data());
    }
}