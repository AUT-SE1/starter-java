package edu.softwarelab.starter.consumer;

import edu.softwarelab.starter.dto.ExecuteExternalTransfer;
import edu.softwarelab.starter.kafka.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class SettlementKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SettlementKafkaConsumer.class);
    @KafkaListener(topics = KafkaTopics.CMD_EXECUTE_TRANSFER, groupId = "s5-stl-group")
    public void consumeExecuteTransfer(
            @Payload ExecuteExternalTransfer command,
            @Header(KafkaHeaders.RECEIVED_KEY) String transactionId,
            @Header(value = "X-Correlation-ID", required = false) String correlationIdHeader) {

        String correlationId = command.getCorrelationId() != null ? command.getCorrelationId() : correlationIdHeader;
        MDC.put("correlation_id", correlationId);

        try {
            log.info("Received ExecuteExternalTransfer command for transaction: {}", transactionId);
            // در اینجا باید متدهای سرویس خود (SettlementService) را برای بررسی Idempotency و ذخیره در دیتابیس فراخوانی کنید
           System.out.println("Received ExecuteExternalTransfer command for transaction: " + transactionId);
        } finally {
            MDC.clear();
        }
    }
}