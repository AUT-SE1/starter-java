package edu.softwarelab.starter.consumer;

import edu.softwarelab.starter.dto.ExecuteExternalTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * دریافت دستور انتقال از S3.
 * مصرف پیام خارج از استوری RTGS است؛ در استوری بعدی فقط باید
 * TransferIngestionService.accept(command) همین‌جا صدا زده شود.
 */
@Component
public class SettlementKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SettlementKafkaConsumer.class);

    @KafkaListener(
            topics = "${kafka.clusters.s3.topics.external-transfer}",
            groupId = "s5-stl-group",
            containerFactory = "s3ListenerFactory")
    public void consumeExecuteTransfer(@Payload ExecuteExternalTransfer command) {
        MDC.put("correlation_id", command.correlationId());
        try {
            log.info("Received ExecuteExternalTransfer for transaction: {}", command.transactionId());
            // TODO(استوری مصرف از Kafka): ingestionService.accept(command);
        } finally {
            MDC.clear();
        }
    }
}
