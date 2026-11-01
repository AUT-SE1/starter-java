package edu.softwarelab.starter.consumer;

import edu.softwarelab.starter.dto.report.ReportResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportResultConsumer.class);

    @KafkaListener(
        topics = "lab.s6.s5.event.report-result.v1",
        groupId = "s5-report-result-group"
    )
    public void consumeReportResult(ReportResultEvent event) {
        log.info("Received report acknowledgment from {}. ReportId: {}, Outcome: {}",
                event.producer(), event.data().reportId(), event.data().outcome());

        if ("ACCEPTED".equalsIgnoreCase(event.data().outcome())) {
            log.info("Daily settlement report {} successfully verified by S6.", event.data().reportId());
            // در اینجا می‌توانید وضعیت گزارش را در دیتابیس به ACKNOWLEDGED آپدیت کنید
        } else {
            log.error("Daily settlement report {} was REJECTED by S6! Error: {}",
                    event.data().reportId(), event.data().error() != null ? event.data().error().detail() : "Unknown");
            // آلارم یا آماده‌سازی برای بررسی دستی
        }
    }
}