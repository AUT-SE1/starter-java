package edu.softwarelab.starter.engine;

import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.entity.SettlementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/** شبیه‌ساز تسویه‌ی لحظه‌ای (RTGS) شبکه‌ی بین‌بانکی. */
@Component
public class RtgsSettlementEngine implements SettlementEngine {

    private static final Logger log = LoggerFactory.getLogger(RtgsSettlementEngine.class);
    private static final Pattern IR_IBAN = Pattern.compile("^IR\\d{24}$");

    private final RandomGenerator random;
    private final long latencyMs;
    private final double transientRate;
    private final double failureRate;
    private final long maxAmount;

    public RtgsSettlementEngine(
            RandomGenerator random,
            @Value("${settlement.rtgs.latency-ms:0}") long latencyMs,
            @Value("${settlement.rtgs.transient-failure-rate:0.0}") double transientRate,
            @Value("${settlement.rtgs.failure-rate:0.0}") double failureRate,
            @Value("${settlement.rtgs.max-amount:100000000000}") long maxAmount) {
        this.random = random;
        this.latencyMs = latencyMs;
        this.transientRate = transientRate;
        this.failureRate = failureRate;
        this.maxAmount = maxAmount;
    }

    @Override
    public boolean supports(String settlementType) {
        return SettlementType.RTGS.equals(settlementType);
    }

    @Override
    public SettlementDecision decide(SettlementTransfer t) {
        if (!supports(t.getSettlementType())) {
            throw new IllegalArgumentException("RtgsSettlementEngine cannot process type " + t.getSettlementType());
        }

        // ۱. اعتبارسنجی (خطای دائمی؛ قبل از هر شبیه‌سازی)
        if (t.getAmount() == null || t.getAmount() <= 0) {
            return SettlementDecision.failed("INVALID_AMOUNT", "Amount must be a positive integer");
        }
        if (t.getAmount() > maxAmount) {
            return SettlementDecision.failed("AMOUNT_LIMIT_EXCEEDED", "Amount exceeds the RTGS limit");
        }
        if (!"IRR".equals(t.getCurrency())) {
            return SettlementDecision.failed("INVALID_CURRENCY", "Only IRR is supported");
        }
        if (t.getDestinationIban() == null || !IR_IBAN.matcher(t.getDestinationIban()).matches()) {
            return SettlementDecision.failed("INVALID_DESTINATION_IBAN", "Destination IBAN must be IR followed by 24 digits");
        }

        // ۲. شبیه‌سازی تأخیر شبکه
        simulateLatency();

        // ۳. شبیه‌سازی نتیجه: یک بار قرعه؛ بازه‌ی خطای موقت، بعد خطای تجاری، بقیه موفق
        double draw = random.nextDouble();
        if (draw < transientRate) {
            log.warn("RTGS simulated network timeout for transfer {}", t.getId());
            throw new TransientSettlementException("Simulated interbank network timeout");
        }
        if (draw < transientRate + failureRate) {
            return SettlementDecision.failed("DESTINATION_BANK_REJECTED", "Destination bank rejected the transfer");
        }
        return SettlementDecision.settled();
    }

    private void simulateLatency() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientSettlementException("Interrupted while waiting for interbank network");
        }
    }
}
