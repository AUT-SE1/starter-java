package edu.softwarelab.starter.producer;

import edu.softwarelab.starter.dto.report.SettlementDailyReportEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ReportKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ReportKafkaProducer.class);

    @Value("${kafka.clusters.s6.topics.daily-report}")
    private String dailyReportTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReportKafkaProducer(@Qualifier("s6KafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * ارسال گزارش پایان روز به کلاستر کافکای S6
     * در صورت بروز خطا تا ۳ بار با فاصله ۲ ثانیه تلاش مجدد می‌کند (Retry)
     */
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendDailyReport(SettlementDailyReportEvent report) {
        String messageKey = report.data().reportId().toString();

        log.info("Starting publication of daily report to S6 Kafka. report_id={}, business_date={}, total_records={}",
                report.data().reportId(), report.data().businessDate(), report.data().summary().totalCount());

        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    dailyReportTopic,
                    messageKey,
                    report
            );

            SendResult<String, Object> result = future.get();

            log.info("Daily report successfully published to S6. report_id={}, topic={}, partition={}, offset={}",
                    report.data().reportId(),
                    dailyReportTopic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception ex) {
            log.error("Failed to publish daily report to S6. report_id={}, error={}",
                    report.data().reportId(), ex.getMessage(), ex);
            throw new RuntimeException("Error sending daily report to S6 Kafka: " + ex.getMessage(), ex);
        }
    }

    /**
     * این متد زمانی اجرا می‌شود که هر ۳ تلاش با شکست مواجه شوند.
     */
    @Recover
    public void recover(Exception ex, SettlementDailyReportEvent report) {
        log.error("CRITICAL: Failed to publish daily report to S6 after 3 retries! report_id={}, business_date={}, cause={}",
                report.data().reportId(),
                report.data().businessDate(),
                ex.getMessage());
    }
}