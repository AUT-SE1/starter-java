package edu.softwarelab.starter.service;

import edu.softwarelab.starter.engine.SettlementDecision;
import edu.softwarelab.starter.entity.SettlementStatus;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * هر متد یک تراکنش کوتاه DB است. جدا از SettlementProcessingService نگه داشته شده
 * تا @Transactional روی فراخوانی‌های داخلی هم کار کند و در زمان شبیه‌سازی تأخیر،
 * connection دیتابیس نگه داشته نشود.
 */
@Service
public class SettlementStateService {

    private static final int MAX_REASON_LENGTH = 255;

    private final SettlementTransferRepository repository;
    private final Clock clock;

    public SettlementStateService(SettlementTransferRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** ACCEPTED → PROCESSING. true یعنی این فراخواننده برنده است. */
    @Transactional
    public boolean claim(UUID id) {
        return repository.claim(id, OffsetDateTime.now(clock)) == 1;
    }

    /** PROCESSING → SETTLED یا FAILED. false یعنی قبلاً نهایی شده و چیزی نوشته نشد. */
    @Transactional
    public boolean finalizeTransfer(UUID id, SettlementDecision decision) {
        String status = decision.outcome() == SettlementDecision.Outcome.SETTLED
                ? SettlementStatus.SETTLED
                : SettlementStatus.FAILED;
        return repository.finalizeTransfer(
                id, status, OffsetDateTime.now(clock),
                decision.failureCode(), truncate(decision.failureReason())) == 1;
    }

    /** PROCESSING → ACCEPTED برای تلاش بعدی. */
    @Transactional
    public boolean release(UUID id) {
        return repository.release(id) == 1;
    }

    /** PROCESSINGهای قدیمی‌تر از timeout را به ACCEPTED برمی‌گرداند. */
    @Transactional
    public int recoverStuck(Duration timeout) {
        return repository.releaseStuck(OffsetDateTime.now(clock).minus(timeout));
    }

    private static String truncate(String s) {
        if (s == null || s.length() <= MAX_REASON_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_REASON_LENGTH);
    }
}
