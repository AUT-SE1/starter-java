package edu.softwarelab.starter.producer;

import edu.softwarelab.starter.dto.ExecuteExternalTransferResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * ارسال نتیجه به S3 (کلید پیام: transaction_id).
 * موتور تسویه و SettlementProcessingService عمداً از این کلاس استفاده نمی‌کنند
 * (انتشار نتیجه استوری جداست).
 */
@Component
public class SettlementKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(SettlementKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String operationResultTopic;

    public SettlementKafkaProducer(
            @Qualifier("s3KafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.clusters.s3.topics.operation-result}") String operationResultTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.operationResultTopic = operationResultTopic;
    }

    public void sendTransferResult(ExecuteExternalTransferResult result) {
        log.info("Sending ExecuteExternalTransferResult for transactionId: {}", result.transactionId());
        kafkaTemplate.send(operationResultTopic, result.transactionId().toString(), result);
    }
}
