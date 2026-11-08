package edu.softwarelab.starter.integration;

import edu.softwarelab.starter.dto.SettlementDailyReportEvent;
import edu.softwarelab.starter.entity.SettlementStatus;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import edu.softwarelab.starter.service.SettlementReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SettlementReportIntegrationTest {

    private static final ZoneId TEHRAN_ZONE = ZoneId.of("Asia/Tehran");

    @Autowired
    private SettlementReportService reportService;

    @Autowired
    private SettlementTransferRepository repository;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 10, 20);
        repository.deleteAll();
    }

    private SettlementTransfer persistTransfer(Long amount, String status, OffsetDateTime settledAt) {
        SettlementTransfer transfer = new SettlementTransfer();
        transfer.setId(UUID.randomUUID());
        transfer.setIdempotencyKey("key-" + UUID.randomUUID());
        transfer.setCorrelationId(UUID.randomUUID().toString());
        transfer.setSourceIban("IR111111111111111111111111");
        transfer.setDestinationIban("IR222222222222222222222222");
        transfer.setAmount(amount);
        transfer.setCurrency("IRR");
        transfer.setSettlementType("PAYA");
        transfer.setStatus(status); // استفاده از وضعیت معتبر پایگاه داده: SETTLED
        transfer.setCreatedAt(settledAt.minusMinutes(10));
        transfer.setSettledAt(settledAt);
        return repository.save(transfer);
    }

    @Test
    @DisplayName("Integration 1: واکشی تراکنش‌های نهایی‌شده با وضعیت SETTLED از دیتابیس واقعی")
    void testFetchAndAggregateFromDatabase() {
        OffsetDateTime settledTime = testDate.atTime(10, 30).atZone(TEHRAN_ZONE).toOffsetDateTime();
        persistTransfer(350_000L, SettlementStatus.SETTLED, settledTime);

        SettlementDailyReportEvent report = reportService.generateDailyReport(testDate, UUID.randomUUID(), UUID.randomUUID());

        assertThat(report.data().transactions()).hasSize(1);
        assertThat(report.data().summary().totalAmount()).isEqualTo(350_000L);
        assertThat(report.data().summary().totalCount()).isEqualTo(1);
        assertThat(report.data().summary().successCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Integration 2: فیلتر کردن دقیق بازه زمانی تاریخ تهران و عدم ورود سایر روزها")
    void testDateFilterExcludesOtherDates() {
        OffsetDateTime todaySettled = testDate.atTime(12, 0).atZone(TEHRAN_ZONE).toOffsetDateTime();
        OffsetDateTime tomorrowSettled = testDate.plusDays(1).atTime(12, 0).atZone(TEHRAN_ZONE).toOffsetDateTime();

        persistTransfer(100_000L, SettlementStatus.SETTLED, todaySettled);
        persistTransfer(900_000L, SettlementStatus.SETTLED, tomorrowSettled);

        SettlementDailyReportEvent report = reportService.generateDailyReport(testDate, UUID.randomUUID(), UUID.randomUUID());

        assertThat(report.data().summary().totalCount()).isEqualTo(1);
        assertThat(report.data().summary().totalAmount()).isEqualTo(100_000L);
    }
}