package edu.softwarelab.starter.entity;

/** وضعیت‌های مجاز تراکنش تسویه (باید با constraint در migration یکی باشد). */
public final class SettlementStatus {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String PROCESSING = "PROCESSING";
    public static final String SETTLED = "SETTLED";
    public static final String FAILED = "FAILED";
    public static final String SETTLED_CONFIRMED = "SETTLED_CONFIRMED";

    private SettlementStatus() {
    }
}
