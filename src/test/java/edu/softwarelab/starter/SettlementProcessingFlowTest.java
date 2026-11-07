package edu.softwarelab.starter;

import edu.softwarelab.starter.dto.ExecuteExternalTransfer;
import edu.softwarelab.starter.engine.RtgsSettlementEngine;
import edu.softwarelab.starter.engine.SettlementDecision;
import edu.softwarelab.starter.engine.TransientSettlementException;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.producer.SettlementKafkaProducer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import edu.softwarelab.starter.service.SettlementProcessingService;
import edu.softwarelab.starter.service.SettlementProcessingService.ProcessingOutcome;
import edu.softwarelab.starter.service.SettlementStateService;
import edu.softwarelab.starter.service.TransferIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** سناریوهای معیار پذیرش استوری RTGS، روی Postgres واقعی و موتور mock شده. */
class SettlementProcessingFlowTest extends PostgresIntegrationTest {

    private static final String IBAN = "IR000120000000000000000002";

    @MockBean
    RtgsSettlementEngine engine;

    /** اگر موتور/سرویس‌ها به Kafka چیزی بفرستند، تست می‌شکند (معیار ۷). */
    @MockBean
    SettlementKafkaProducer s3Producer;

    @Autowired SettlementProcessingService processing;
    @Autowired SettlementStateService state;
    @Autowired TransferIngestionService ingestion;
    @Autowired SettlementTransferRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(engine.supports(anyString())).thenAnswer(inv -> "RTGS".equals(inv.getArgument(0)));
        when(engine.decide(any())).thenReturn(SettlementDecision.settled());
    }

    private SettlementTransfer save(String type, String status) {
        SettlementTransfer t = new SettlementTransfer();
        t.setId(UUID.randomUUID());
        t.setIdempotencyKey("idem-" + UUID.randomUUID());
        t.setCorrelationId("corr");
        t.setDestinationIban(IBAN);
        t.setAmount(2_500_000L);
        t.setSettlementType(type);
        t.setStatus(status);
        return repository.save(t);
    }

    private SettlementTransfer reload(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    private ExecuteExternalTransfer command(UUID transactionId, UUID operationId) {
        return new ExecuteExternalTransfer(UUID.randomUUID(), "3.0", transactionId, "corr-1", null,
                OffsetDateTime.now(), "S3-PAY", "ExecuteExternalTransfer.v3", operationId,
                new ExecuteExternalTransfer.Data(IBAN, 2_500_000L, "IRR"));
    }

    @Test
    void settlesAcceptedRtgsTransfer() {
        SettlementTransfer t = save("RTGS", "ACCEPTED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.SETTLED);

        SettlementTransfer after = reload(t.getId());
        assertThat(after.getStatus()).isEqualTo("SETTLED");
        assertThat(after.getSettledAt()).isNotNull();
        assertThat(after.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void recordsBusinessFailure() {
        when(engine.decide(any())).thenReturn(SettlementDecision.failed("DESTINATION_BANK_REJECTED", "no"));
        SettlementTransfer t = save("RTGS", "ACCEPTED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.FAILED);

        SettlementTransfer after = reload(t.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailureCode()).isEqualTo("DESTINATION_BANK_REJECTED");
        assertThat(after.getSettledAt()).isNotNull(); // تا در گزارش روزانه بیاید
    }

    @Test
    void finalizedTransferIsNeverReprocessed() {
        SettlementTransfer t = save("RTGS", "SETTLED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.SKIPPED);

        verify(engine, never()).decide(any());
        assertThat(reload(t.getId()).getStatus()).isEqualTo("SETTLED");
    }

    @Test
    void nonRtgsTransferIsLeftAlone() {
        SettlementTransfer t = save("PAYA", "ACCEPTED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.SKIPPED);

        verify(engine, never()).decide(any());
        assertThat(reload(t.getId()).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void transientFailureDoesNotFinalizeAndRetrySucceeds() {
        when(engine.decide(any()))
                .thenThrow(new TransientSettlementException("timeout"))
                .thenReturn(SettlementDecision.settled());
        SettlementTransfer t = save("RTGS", "ACCEPTED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.RETRY_LATER);
        SettlementTransfer mid = reload(t.getId());
        assertThat(mid.getStatus()).isEqualTo("ACCEPTED"); // نه FAILED، نه نتیجه‌ی مالی
        assertThat(mid.getSettledAt()).isNull();

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.SETTLED);
        SettlementTransfer after = reload(t.getId());
        assertThat(after.getStatus()).isEqualTo("SETTLED");
        assertThat(after.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void repeatedTransientFailuresEndInRetryExhausted() {
        when(engine.decide(any())).thenThrow(new TransientSettlementException("timeout"));
        SettlementTransfer t = save("RTGS", "ACCEPTED");

        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.RETRY_LATER);
        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.RETRY_LATER);
        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.FAILED); // max-attempts=3

        SettlementTransfer after = reload(t.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailureCode()).isEqualTo("RETRY_EXHAUSTED");
    }

    @Test
    void concurrentProcessingYieldsExactlyOneResult() throws Exception {
        AtomicInteger decideCalls = new AtomicInteger();
        when(engine.decide(any())).thenAnswer(inv -> {
            decideCalls.incrementAndGet();
            Thread.sleep(100); // پنجره‌ی رقابت را بزرگ می‌کند
            return SettlementDecision.settled();
        });
        SettlementTransfer t = save("RTGS", "ACCEPTED");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ProcessingOutcome>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return processing.process(t.getId());
            }));
        }
        start.countDown();
        int settled = 0;
        for (Future<ProcessingOutcome> f : futures) {
            if (f.get(10, TimeUnit.SECONDS) == ProcessingOutcome.SETTLED) {
                settled++;
            }
        }
        pool.shutdown();

        assertThat(settled).isEqualTo(1);
        assertThat(decideCalls.get()).isEqualTo(1);
        assertThat(reload(t.getId()).getStatus()).isEqualTo("SETTLED");
    }

    @Test
    void stuckProcessingTransferIsRecovered() {
        SettlementTransfer t = save("RTGS", "PROCESSING");
        t.setProcessingStartedAt(OffsetDateTime.now().minusMinutes(10));
        repository.save(t);

        assertThat(state.recoverStuck(Duration.ofMinutes(1))).isEqualTo(1);

        assertThat(reload(t.getId()).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void ingestionPersistsFirstThenProcessesAfterCommit() {
        UUID txId = UUID.randomUUID();
        when(engine.decide(any())).thenAnswer(inv -> {
            SettlementTransfer t = inv.getArgument(0);
            // لحظه‌ی پردازش، تراکنش باید قبلاً در DB باشد (معیار ۲)
            assertThat(repository.findById(t.getId())).isPresent();
            return SettlementDecision.settled();
        });

        ingestion.accept(command(txId, UUID.randomUUID()));

        SettlementTransfer after = reload(txId);
        assertThat(after.getStatus()).isEqualTo("SETTLED");
        assertThat(after.getSettlementType()).isEqualTo("RTGS");
        assertThat(after.getSourceIban()).isNull();
        verifyNoInteractions(s3Producer);
    }

    @Test
    void duplicateCommandIsPersistedAndProcessedOnce() {
        UUID txId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();

        ingestion.accept(command(txId, opId));
        ingestion.accept(command(txId, opId));

        assertThat(repository.count()).isEqualTo(1);
        verify(engine, times(1)).decide(any());
    }
}
