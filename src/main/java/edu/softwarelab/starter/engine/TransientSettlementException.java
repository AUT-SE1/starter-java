package edu.softwarelab.starter.engine;

/**
 * خطای موقت (مثلاً timeout شبکه‌ی بین‌بانکی). نتیجه‌ی نهایی ساخته نمی‌شود
 * و تراکنش برای تلاش بعدی به ACCEPTED برمی‌گردد.
 */
public class TransientSettlementException extends RuntimeException {
    public TransientSettlementException(String message) {
        super(message);
    }
}
