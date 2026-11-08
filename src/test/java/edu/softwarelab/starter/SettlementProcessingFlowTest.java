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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class SettlementProcessingFlowTest {

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

    // متد save اصلاح شد تا همان String استاندارد پروژه را دریافت کند
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

    // متد Command اصلاح شد تا از TransferData و Setterهای استاندارد استفاده کند
    private ExecuteExternalTransfer command(UUID transactionId, UUID operationId) {
        ExecuteExternalTransfer cmd = new ExecuteExternalTransfer();
        cmd.setTransactionId(transactionId);
        cmd.setOperationId(operationId);
        cmd.setCorrelationId("corr-1");
        cmd.setMessageType("ExecuteExternalTransfer.v3");
        cmd.setProducer("S3-PAY");
        cmd.setSchemaVersion("3.0");
        cmd.setOccurredAt(OffsetDateTime.now());

        ExecuteExternalTransfer.TransferData data = new ExecuteExternalTransfer.TransferData();
        data.setDestIban(IBAN);
        data.setAmount(2_500_000L);
        data.setCurrency("IRR");

        cmd.setData(data);
        return cmd;
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
        assertThat(after.getSettledAt()).isNotNull();
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
        assertThat(mid.getStatus()).isEqualTo("ACCEPTED");
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
        assertThat(processing.process(t.getId())).isEqualTo(ProcessingOutcome.FAILED);

        SettlementTransfer after = reload(t.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailureCode()).isEqualTo("RETRY_EXHAUSTED");
    }

    @Test
    void concurrentProcessingYieldsExactlyOneResult() throws Exception {
        AtomicInteger decideCalls = new AtomicInteger();
        when(engine.decide(any())).thenAnswer(inv -> {
            decideCalls.incrementAndGet();
            Thread.sleep(100);
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