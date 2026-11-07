package edu.softwarelab.starter.controller;

import edu.softwarelab.starter.dto.SettlementDailyReportEvent;
import edu.softwarelab.starter.service.SettlementReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class SettlementReportController {

    private static final Logger log = LoggerFactory.getLogger(SettlementReportController.class);

    private final SettlementReportService reportService;

    public SettlementReportController(SettlementReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * اندپوینت دریافت گزارش تسویه‌های یک روز بر اساس تاریخ
     * GET /settlement-reports?date=2026-10-31
     */
    @GetMapping("/settlement-reports")
    public ResponseEntity<?> getSettlementReport(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // اگر تاریخی پاس داده نشود، به صورت پیش‌فرض تاریخ روز کاری قبل را در نظر می‌گیرد
        LocalDate targetDate = (date != null) ? date : LocalDate.now().minusDays(1);

        log.info("REST request received for settlement report: date={}", targetDate);

        // تولید گزارش با همان متد تجمیعی که برای کافکا استفاده شده است
        SettlementDailyReportEvent reportEvent = reportService.generateDailyReport(
                targetDate,
                UUID.randomUUID(), // correlation_id جدید برای درخواست REST
                null
        );

        // در صورت تمایل می‌توانید کل پاکت (reportEvent) یا فقط بخش دیتای آن (reportEvent.data()) را برگردانید
        return ResponseEntity.ok(reportEvent.data());
    }
}