package edu.softwarelab.starter.kafka;

import edu.softwarelab.starter.consumer.ReportInquiryConsumer;
import edu.softwarelab.starter.dto.*;
import edu.softwarelab.starter.producer.ReportKafkaProducer;
import edu.softwarelab.starter.service.SettlementReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S6KafkaComponentsTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReportKafkaProducer producer;

    @Mock
    private SettlementReportService reportService;

    @Mock
    private ReportKafkaProducer s6Producer;

    @InjectMocks
    private ReportInquiryConsumer consumer;

    @Test
    @DisplayName("Kafka 1: انتشار موفق رویداد گزارش روزانه به کافکای S6 توسط پرودوسر")
    void testProducerPublishesToKafka() {
        UUID reportId = UUID.randomUUID();
        SettlementDailyReportEvent event = new SettlementDailyReportEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "S5-STL",
                "SettlementDailyReportEvent.v1",
                new SettlementDailyReportEvent.ReportData(
                        reportId,
                        LocalDate.now(),
                        OffsetDateTime.now(),
                        new SettlementReportSummary(0L, 0L, 0L, 0L),
                        Collections.emptyList()
                )
        );

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        SendResult<String, Object> sendResult = mock(SendResult.class, RETURNS_DEEP_STUBS);
        future.complete(sendResult);

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        producer.sendDailyReport(event);

        verify(kafkaTemplate, times(1)).send(any(), eq(reportId.toString()), eq(event));
    }

    @Test
    @DisplayName("Kafka 2: مصرف مستقیم دستور استعلام، فراخوانی سرویس و ارسال نتیجه به پرودوسر")
    void testConsumerHandlesInquiryCommandDirectly() {
        LocalDate businessDate = LocalDate.of(2026, 10, 20);
        UUID messageId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        ReportInquiryCommand command = new ReportInquiryCommand(
                messageId,
                correlationId,
                OffsetDateTime.now(),
                "S6",
                "ReportInquiryCommand.v1",
                new ReportInquiryCommand.InquiryData(businessDate, null)
        );

        SettlementDailyReportEvent generatedReport = new SettlementDailyReportEvent(
                UUID.randomUUID(),
                correlationId,
                messageId,
                "S5-STL",
                "SettlementDailyReportEvent.v1",
                new SettlementDailyReportEvent.ReportData(
                        UUID.randomUUID(),
                        businessDate,
                        OffsetDateTime.now(),
                        new SettlementReportSummary(1L, 1000L, 1L, 0L),
                        Collections.emptyList()
                )
        );

        when(reportService.generateDailyReport(businessDate, correlationId, messageId))
                .thenReturn(generatedReport);

        consumer.consumeInquiry(command);

        verify(reportService, times(1)).generateDailyReport(businessDate, correlationId, messageId);
        verify(s6Producer, times(1)).sendDailyReport(generatedReport);
    }

    @Test
    @DisplayName("Kafka 3: نادیده گرفتن دستور استعلام در صورت ارسال فیلد null برای تاریخ")
    void testConsumerIgnoresNullBusinessDate() {
        ReportInquiryCommand command = new ReportInquiryCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                "S6",
                "ReportInquiryCommand.v1",
                new ReportInquiryCommand.InquiryData(null, null)
        );

        consumer.consumeInquiry(command);

        verifyNoInteractions(reportService);
        verifyNoInteractions(s6Producer);
    }
}