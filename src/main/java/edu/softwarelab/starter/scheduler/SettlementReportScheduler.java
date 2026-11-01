package edu.softwarelab.starter.scheduler;

import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import edu.softwarelab.starter.producer.ReportKafkaProducer;
import edu.softwarelab.starter.service.SettlementReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SettlementReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementReportScheduler.class);
    private static final ZoneId TEHRAN_ZONE = ZoneId.of("Asia/Tehran");

    private final SettlementReportService reportService;
    private final ReportKafkaProducer kafkaProducer;

    // نگهداری آخرین تاریخ با ارسال موفق در حافظه برای جلوگیری از اجرای تکراری
    private LocalDate lastProcessedDate = null;

    public SettlementReportScheduler(SettlementReportService reportService,
                                     ReportKafkaProducer kafkaProducer) {
        this.reportService = reportService;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(cron = "${settlement.report.cron:0 0 0 * * *}", zone = "Asia/Tehran")
    public void runEndOfDayReport() {
        LocalDate targetDate = LocalDate.now(TEHRAN_ZONE).minusDays(1);

        // ۱. بررسی عدم اجرای تکراری برای یک تاریخ کاری
        if (targetDate.equals(lastProcessedDate)) {
            log.warn("Daily report for business_date={} has already been successfully published. Skipping.", targetDate);
            return;
        }

        log.info("Starting End-of-Day Report Job for business_date={}", targetDate);

        try {
            // ۲. تولید گزارش
            SettlementDailyReportEvent report = reportService.generateDailyReport(targetDate);
            log.info("Report generated successfully. report_id={}, transactions_count={}",
                    report.data().reportId(), report.data().transactions().size());

            // ۳. انتشار در کافکا (همراه با مکانیزم Retry)
            kafkaProducer.sendDailyReport(report);

            // ۴. ذخیره تاریخ پردازش‌شده تنها پس از موفقیت قطعی ارسال
            this.lastProcessedDate = targetDate;

            log.info("End-of-Day Report Job completed successfully for business_date={}", targetDate);
        } catch (Exception e) {
            log.error("End-of-Day Report Job failed for business_date={}. Error: {}", targetDate, e.getMessage(), e);
        }
    }
}