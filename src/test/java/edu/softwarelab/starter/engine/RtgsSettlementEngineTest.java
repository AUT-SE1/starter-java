package edu.softwarelab.starter.engine;

import edu.softwarelab.starter.entity.SettlementTransfer;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** تست واحد خالص (بدون Spring و DB). */
class RtgsSettlementEngineTest {

    private static final String VALID_IBAN = "IR000120000000000000000002";

    /** قرعه‌ی ثابت: transient برای <0.10 ، خطای تجاری برای <0.30 ، وگرنه موفق. */
    private static RtgsSettlementEngine engineWithDraw(double draw) {
        RandomGenerator fixed = new RandomGenerator() {
            @Override public long nextLong() { return 0L; }
            @Override public double nextDouble() { return draw; }
        };
        return new RtgsSettlementEngine(fixed, 0L, 0.10, 0.20, 1_000_000_000L);
    }

    private static SettlementTransfer transfer() {
        SettlementTransfer t = new SettlementTransfer();
        t.setSettlementType("RTGS");
        t.setDestinationIban(VALID_IBAN);
        t.setAmount(2_500_000L);
        t.setCurrency("IRR");
        return t;
    }

    @Test
    void supportsOnlyRtgs() {
        RtgsSettlementEngine engine = engineWithDraw(0.9);
        assertThat(engine.supports("RTGS")).isTrue();
        assertThat(engine.supports("PAYA")).isFalse();
        assertThat(engine.supports("SATNA")).isFalse();
    }

    @Test
    void refusesToProcessOtherTypes() {
        SettlementTransfer t = transfer();
        t.setSettlementType("PAYA");
        assertThatThrownBy(() -> engineWithDraw(0.9).decide(t)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validTransferIsSettled() {
        SettlementDecision d = engineWithDraw(0.9).decide(transfer());
        assertThat(d.outcome()).isEqualTo(SettlementDecision.Outcome.SETTLED);
        assertThat(d.failureCode()).isNull();
    }

    @Test
    void businessFailureIsFailedWithCode() {
        SettlementDecision d = engineWithDraw(0.20).decide(transfer());
        assertThat(d.outcome()).isEqualTo(SettlementDecision.Outcome.FAILED);
        assertThat(d.failureCode()).isEqualTo("DESTINATION_BANK_REJECTED");
    }

    @Test
    void transientFailureThrowsAndProducesNoDecision() {
        assertThatThrownBy(() -> engineWithDraw(0.05).decide(transfer()))
                .isInstanceOf(TransientSettlementException.class);
    }

    // قرعه‌ی 0.05 یعنی اگر اعتبارسنجی نبود، transient می‌شد؛ پس اعتبارسنجی قبل از شبیه‌سازی است.

    @Test
    void nonPositiveAmountIsRejectedBeforeSimulation() {
        SettlementTransfer t = transfer();
        t.setAmount(0L);
        assertThat(engineWithDraw(0.05).decide(t).failureCode()).isEqualTo("INVALID_AMOUNT");
    }

    @Test
    void nullAmountIsRejected() {
        SettlementTransfer t = transfer();
        t.setAmount(null);
        assertThat(engineWithDraw(0.05).decide(t).failureCode()).isEqualTo("INVALID_AMOUNT");
    }

    @Test
    void amountAboveLimitIsRejected() {
        SettlementTransfer t = transfer();
        t.setAmount(1_000_000_001L);
        assertThat(engineWithDraw(0.05).decide(t).failureCode()).isEqualTo("AMOUNT_LIMIT_EXCEEDED");
    }

    @Test
    void nonIrrCurrencyIsRejected() {
        SettlementTransfer t = transfer();
        t.setCurrency("USD");
        assertThat(engineWithDraw(0.05).decide(t).failureCode()).isEqualTo("INVALID_CURRENCY");
    }

    @Test
    void malformedIbanIsRejected() {
        SettlementTransfer t = transfer();
        t.setDestinationIban("IR12345");
        assertThat(engineWithDraw(0.05).decide(t).failureCode()).isEqualTo("INVALID_DESTINATION_IBAN");
    }
}
