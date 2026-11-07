package edu.softwarelab.starter.kafka;

import edu.softwarelab.starter.dto.ExecuteExternalTransferResult;
import edu.softwarelab.starter.producer.SettlementKafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SettlementKafkaProducer producer;

    @Test
    void testSendTransferResult_ShouldCallKafkaTemplate() {
        // آماده‌سازی داده‌های تست
        ExecuteExternalTransferResult result = new ExecuteExternalTransferResult();
        UUID transactionId = UUID.randomUUID();
        result.setTransactionId(transactionId);
        result.setOutcome("APPLIED");

        // فراخوانی متد مورد نظر
        producer.sendTransferResult(result);

        // بررسی اینکه آیا متد send در KafkaTemplate دقیقاً یک بار با پارامترهای درست صدا زده شده است یا خیر
        verify(kafkaTemplate, times(1)).send(
                KafkaTopics.EVT_OPERATION_RESULT,
                transactionId.toString(),
                result
        );
    }
}