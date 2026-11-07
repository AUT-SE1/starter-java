package edu.softwarelab.starter.consumer;

import edu.softwarelab.starter.dto.ReportResultEvent;
import edu.softwarelab.starter.service.SettlementReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportResultConsumer.class);

    private final SettlementReportService reportService;

    public ReportResultConsumer(SettlementReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(
            topics = "${kafka.clusters.s6.topics.report-result}",
            groupId = "s5-report-result-group",
            containerFactory = "s6ListenerFactory")
    public void consumeReportResult(ReportResultEvent event) {
        if (event == null || event.data() == null) {
            log.warn("Ignored empty report result");
            return;
        }
        log.info("Received report result from {}: report_id={}, outcome={}",
                event.producer(), event.data().reportId(), event.data().outcome());
        // تأیید ACCEPTED وضعیت را به SETTLED_CONFIRMED می‌برد؛ REJECTED لاگ می‌شود.
        reportService.processReportResult(event);
    }
}
