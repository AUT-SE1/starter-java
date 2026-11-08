package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.SettlementDailyReportEvent;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementReportServiceTest {

    @Mock
    private SettlementTransferRepository repository;

    @InjectMocks
    private SettlementReportService reportService;

    private LocalDate testDate;
    private UUID correlationId;
    private UUID causationId;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 10, 20);
        correlationId = UUID.randomUUID();
        causationId = UUID.randomUUID();
    }

    private SettlementTransfer buildTransfer(Long amount, String status, String failureReason) {
        SettlementTransfer transfer = new SettlementTransfer();
        transfer.setId(UUID.randomUUID());
        transfer.setIdempotencyKey("idemp-" + UUID.randomUUID());
        transfer.setCorrelationId(correlationId.toString());
        transfer.setSourceIban("IR111111111111111111111111");
        transfer.setDestinationIban("IR222222222222222222222222");
        transfer.setAmount(amount);
        transfer.setCurrency("IRR");
        transfer.setSettlementType("PAYA");
        transfer.setStatus(status); // باید با شروط calculateSummary همخوانی داشته باشد (SUCCESS یا FAILED)
        transfer.setFailureReason(failureReason);
        transfer.setCreatedAt(testDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        transfer.setSettledAt(testDate.atTime(14, 0).atOffset(ZoneOffset.UTC));
        return transfer;
    }

    @Test
    @DisplayName("Unit 1: محاسبه صحیح خلاصه مبالغ تراکنش‌های SUCCESS و شمارش تراکنش‌های FAILED")
    void testCalculateSummaryCorrectly() {
        // وضعیت SUCCESS برای اضافه شدن به جمع مبالغ
        SettlementTransfer tx1 = buildTransfer(150_000L, "SUCCESS", null);
        SettlementTransfer tx2 = buildTransfer(50_000L, "SUCCESS", null);
        SettlementTransfer tx3 = buildTransfer(70_000L, "FAILED", "INSUFFICIENT_FUNDS");

        when(repository.findFinalizedTransfersForReport(any(), any(), any()))
                .thenReturn(List.of(tx1, tx2, tx3));

        SettlementDailyReportEvent report = reportService.generateDailyReport(testDate, correlationId, causationId);

        assertThat(report).isNotNull();
        assertThat(report.data().summary().totalCount()).isEqualTo(3);
        assertThat(report.data().summary().totalAmount()).isEqualTo(200_000L); // 150_000 + 50_000
        assertThat(report.data().summary().successCount()).isEqualTo(2);
        assertThat(report.data().summary().failedCount()).isEqualTo(1);
        assertThat(report.data().transactions()).hasSize(3);
    }

    @Test
    @DisplayName("Unit 2: بازگرداندن مقادیر صفر در صورت نبود هیچ تراکنشی در تاریخ مربوطه")
    void testEmptyTransactionsReport() {
        when(repository.findFinalizedTransfersForReport(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        SettlementDailyReportEvent report = reportService.generateDailyReport(testDate, correlationId, causationId);

        assertThat(report).isNotNull();
        assertThat(report.data().summary().totalCount()).isZero();
        assertThat(report.data().summary().totalAmount()).isZero();
        assertThat(report.data().summary().successCount()).isZero();
        assertThat(report.data().summary().failedCount()).isZero();
        assertThat(report.data().transactions()).isEmpty();
    }

    @Test
    @DisplayName("Unit 3: انطباق دقیق شناسه‌های Correlation و Causation")
    void testCorrelationAndCausationMapping() {
        when(repository.findFinalizedTransfersForReport(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        SettlementDailyReportEvent report = reportService.generateDailyReport(testDate, correlationId, causationId);

        assertThat(report.correlationId()).isEqualTo(correlationId);
        assertThat(report.causationId()).isEqualTo(causationId);
        assertThat(report.data().businessDate()).isEqualTo(testDate);
    }

    @Test
    @DisplayName("Unit 4: تولید شناسه گزارش یکتا بر اساس تاریخ (Deterministic UUID)")
    void testUniqueReportIdGeneration() {
        when(repository.findFinalizedTransfersForReport(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDate otherDate = testDate.plusDays(1);

        SettlementDailyReportEvent r1 = reportService.generateDailyReport(testDate, null, null);
        SettlementDailyReportEvent r2 = reportService.generateDailyReport(otherDate, null, null);

        // شناسه‌ها برای دو روز متفاوت باید مجزا باشند
        assertThat(r1.data().reportId()).isNotNull();
        assertThat(r2.data().reportId()).isNotNull();
        assertThat(r1.data().reportId()).isNotEqualTo(r2.data().reportId());
    }
}