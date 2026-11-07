package edu.softwarelab.starter.engine;

/** تصمیم موتور برای یک تراکنش. موتور فقط تصمیم می‌گیرد؛ ثبت در DB کار سرویس‌هاست. */
public record SettlementDecision(Outcome outcome, String failureCode, String failureReason) {

    public enum Outcome { SETTLED, FAILED }

    public static SettlementDecision settled() {
        return new SettlementDecision(Outcome.SETTLED, null, null);
    }

    public static SettlementDecision failed(String code, String reason) {
        return new SettlementDecision(Outcome.FAILED, code, reason);
    }
}
