package edu.softwarelab.starter.kafka;

import edu.softwarelab.starter.dto.ExecuteExternalTransferResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SettlementKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(SettlementKafkaProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ارسال پاسخ انتقال وجه به S3 (کلید پیام: transaction_id)
    public void sendTransferResult(ExecuteExternalTransferResult result) {
        log.info("Sending ExecuteExternalTransferResult for transactionId: {}", result.getTransactionId());
        // ارسال پاسخ به تاپیک نتیجه عملیات
        kafkaTemplate.send(KafkaTopics.EVT_OPERATION_RESULT, result.getTransactionId(), result);
    }
}