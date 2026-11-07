package edu.softwarelab.starter.repository;

import edu.softwarelab.starter.entity.SettlementTransfer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, UUID> {

    Optional<SettlementTransfer> findByIdempotencyKey(String idempotencyKey);

    // ---------- گزارش روزانه ----------

    @Query("""
        SELECT t FROM SettlementTransfer t
        WHERE t.settledAt >= :fromDate
          AND t.settledAt < :toDate
          AND t.status IN :statuses
        ORDER BY t.settledAt ASC
    """)
    List<SettlementTransfer> findFinalizedTransfersForReport(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            @Param("statuses") Collection<String> statuses
    );

    /** هم‌شرط با گزارش (settledAt)، تا همان تراکنش‌هایی که در گزارش بوده‌اند تأیید شوند. */
    @Modifying
    @Query("UPDATE SettlementTransfer t SET t.status = :newStatus " +
           "WHERE t.settledAt >= :fromDate AND t.settledAt < :toDate AND t.status = :currentStatus")
    int updateTransfersStatusForBusinessDate(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            @Param("currentStatus") String currentStatus,
            @Param("newStatus") String newStatus
    );

    // ---------- موتور تسویه: آپدیت‌های شرطی و اتمی ----------

    /** فقط یک پردازنده می‌تواند ACCEPTED را به PROCESSING ببرد. نتیجه 1 یعنی برنده شدیم. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SettlementTransfer t SET t.status = 'PROCESSING', " +
           "t.processingStartedAt = :now, t.attemptCount = t.attemptCount + 1 " +
           "WHERE t.id = :id AND t.status = 'ACCEPTED'")
    int claim(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    /** نتیجه‌ی نهایی فقط از PROCESSING نوشته می‌شود؛ پس هر تراکنش حداکثر یک نتیجه‌ی نهایی دارد. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SettlementTransfer t SET t.status = :status, t.settledAt = :now, " +
           "t.failureCode = :code, t.failureReason = :reason " +
           "WHERE t.id = :id AND t.status = 'PROCESSING'")
    int finalizeTransfer(@Param("id") UUID id,
                         @Param("status") String status,
                         @Param("now") OffsetDateTime now,
                         @Param("code") String code,
                         @Param("reason") String reason);

    /** بعد از خطای موقت، تراکنش برای تلاش بعدی به ACCEPTED برمی‌گردد. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SettlementTransfer t SET t.status = 'ACCEPTED', t.processingStartedAt = NULL " +
           "WHERE t.id = :id AND t.status = 'PROCESSING'")
    int release(@Param("id") UUID id);

    /** بازیابی تراکنش‌هایی که پروسه وسط پردازش‌شان کرش کرده است. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SettlementTransfer t SET t.status = 'ACCEPTED', t.processingStartedAt = NULL " +
           "WHERE t.status = 'PROCESSING' AND t.processingStartedAt < :cutoff")
    int releaseStuck(@Param("cutoff") OffsetDateTime cutoff);

    @Query("SELECT t.id FROM SettlementTransfer t " +
           "WHERE t.status = 'ACCEPTED' AND t.settlementType = :type ORDER BY t.createdAt ASC")
    List<UUID> findAcceptedIdsByType(@Param("type") String type, Pageable pageable);
}
