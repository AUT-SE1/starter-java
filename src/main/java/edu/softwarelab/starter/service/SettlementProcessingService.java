package edu.softwarelab.starter.service;

import edu.softwarelab.starter.engine.SettlementDecision;
import edu.softwarelab.starter.engine.SettlementEngine;
import edu.softwarelab.starter.entity.SettlementStatus;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.entity.SettlementType;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * هماهنگ‌کننده‌ی پردازش یک تراکنش: claim → موتور (بیرون از تراکنش DB) → ثبت نتیجه.
 * عمداً @Transactional نیست. این سرویس هیچ پیامی به Kafka/S3 نمی‌فرستد.
 */
@Service
public class SettlementProcessingService {

    public enum ProcessingOutcome { SETTLED, FAILED, SKIPPED, RETRY_LATER }

    private static final Logger log = LoggerFactory.getLogger(SettlementProcessingService.class);

    private final SettlementTransferRepository repository;
    private final SettlementStateService state;
    private final List<SettlementEngine> engines;
    private final int maxAttempts;
    private final long stuckTimeoutSeconds;

    public SettlementProcessingService(
            SettlementTransferRepository repository,
            SettlementStateService state,
            List<SettlementEngine> engines,
            @Value("${settlement.rtgs.max-attempts:3}") int maxAttempts,
            @Value("${settlement.rtgs.stuck-timeout-seconds:60}") long stuckTimeoutSeconds) {
        this.repository = repository;
        this.state = state;
        this.engines = engines;
        this.maxAttempts = maxAttempts;
        this.stuckTimeoutSeconds = stuckTimeoutSeconds;
    }

    public ProcessingOutcome process(UUID transferId) {
        SettlementTransfer transfer = repository.findById(transferId).orElse(null);
        if (transfer == null) {
            log.warn("Transfer {} not found; nothing to process", transferId);
            return ProcessingOutcome.SKIPPED;
        }
        if (!SettlementStatus.ACCEPTED.equals(transfer.getStatus())) {
            log.info("Transfer {} is {}; not reprocessing", transferId, transfer.getStatus());
            return ProcessingOutcome.SKIPPED;
        }
        SettlementEngine engine = engines.stream()
                .filter(e -> e.supports(transfer.getSettlementType()))
                .findFirst()
                .orElse(null);
        if (engine == null) {
            log.debug("No engine supports type {} (transfer {})", transfer.getSettlementType(), transferId);
            return ProcessingOutcome.SKIPPED;
        }
        if (!state.claim(transferId)) {
            log.info("Transfer {} was claimed or finalized by someone else", transferId);
            return ProcessingOutcome.SKIPPED;
        }

        int attempt = transfer.getAttemptCount() + 1; // claim یک واحد اضافه کرده است
        SettlementDecision decision;
        try {
            decision = engine.decide(transfer);
        } catch (RuntimeException ex) {
            // هر خطای غیرمنتظره مثل خطای موقت رفتار می‌کند: نتیجه‌ی نهایی ساخته نمی‌شود.
            if (attempt >= maxAttempts) {
                log.error("Transfer {} failed {} times; marking FAILED", transferId, attempt, ex);
                state.finalizeTransfer(transferId, SettlementDecision.failed(
                        "RETRY_EXHAUSTED", "Settlement failed after " + attempt + " attempts"));
                return ProcessingOutcome.FAILED;
            }
            log.warn("Transient failure for transfer {} (attempt {}): {}", transferId, attempt, ex.getMessage());
            state.release(transferId);
            return ProcessingOutcome.RETRY_LATER;
        }

        if (!state.finalizeTransfer(transferId, decision)) {
            log.warn("Transfer {} was finalized concurrently; result discarded", transferId);
            return ProcessingOutcome.SKIPPED;
        }
        return decision.outcome() == SettlementDecision.Outcome.SETTLED
                ? ProcessingOutcome.SETTLED
                : ProcessingOutcome.FAILED;
    }

    /** برای فراخوانی دستی یا scheduler آینده: PROCESSINGهای گیرکرده را آزاد می‌کند. */
    public int recoverStuck() {
        return state.recoverStuck(Duration.ofSeconds(stuckTimeoutSeconds));
    }

    /** برای فراخوانی دستی یا scheduler آینده: RTGSهای ACCEPTED مانده را دوباره پردازش می‌کند. */
    public int retryPending(int limit) {
        List<UUID> ids = repository.findAcceptedIdsByType(SettlementType.RTGS, PageRequest.of(0, limit));
        int processed = 0;
        for (UUID id : ids) {
            if (process(id) != ProcessingOutcome.SKIPPED) {
                processed++;
            }
        }
        return processed;
    }
}
