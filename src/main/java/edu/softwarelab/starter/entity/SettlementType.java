package edu.softwarelab.starter.entity;

/** نوع تسویه. RTGS = پردازش لحظه‌ای، PAYA = دسته‌ای. */
public final class SettlementType {
    public static final String RTGS = "RTGS";
    public static final String SATNA = "SATNA";
    public static final String PAYA = "PAYA";

    private SettlementType() {
    }
}
