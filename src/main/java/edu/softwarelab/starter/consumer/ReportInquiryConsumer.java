package edu.softwarelab.starter.consumer;

import edu.softwarelab.starter.dto.report.ReportInquiryCommand;
import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import edu.softwarelab.starter.producer.ReportKafkaProducer;
import edu.softwarelab.starter.service.SettlementReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportInquiryConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportInquiryConsumer.class);

    private final SettlementReportService reportService;
    private final ReportKafkaProducer s6Producer;

    public ReportInquiryConsumer(SettlementReportService reportService, ReportKafkaProducer s6Producer) {
        this.reportService = reportService;
        this.s6Producer = s6Producer;
    }

    @KafkaListener(
            topics = "${kafka.clusters.s6.topics.report-inquire}",
            groupId = "s5-report-inquiry-group"
    )
    public void consumeInquiry(ReportInquiryCommand command) {
        log.info("Received inquiry from S6: message_id={}, business_date={}",
                command.messageId(),
                command.data() != null ? command.data().businessDate() : null);

        if (command.data() == null || command.data().businessDate() == null) {
            log.warn("Ignored inquiry: business_date is null.");
            return;
        }

        LocalDate requestedDate = command.data().businessDate();

        // ۱. استفاده از متد ۳ پارامتری برای ست کردن correlation_id و causation_id
        SettlementDailyReportEvent report = reportService.generateDailyReport(
                requestedDate,
                command.correlationId(),
                command.messageId() // شناسه این پیام استعلام، علت (causation) تولید گزارش جدید است
        );

        // ۲. ارسال مستقیم نتیجه تولیدشده به کافکای S6
        s6Producer.sendDailyReport(report);

        log.info("Inquiry fulfilled for date: {}, report_id: {}", requestedDate, report.data().reportId());
    }
}