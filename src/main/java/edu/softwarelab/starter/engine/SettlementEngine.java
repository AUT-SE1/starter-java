package edu.softwarelab.starter.engine;

import edu.softwarelab.starter.entity.SettlementTransfer;

/**
 * قرارداد موتور تسویه. پیاده‌سازی‌ها نباید به DB یا Kafka دسترسی داشته باشند
 * (انتشار نتیجه برای S3 در استوری جدا انجام می‌شود).
 */
public interface SettlementEngine {

    /** آیا این موتور این نوع تسویه را پردازش می‌کند؟ */
    boolean supports(String settlementType);

    /**
     * @return تصمیم نهایی (موفق یا ناموفق)
     * @throws TransientSettlementException اگر خطا موقتی بود و باید بعداً دوباره تلاش شود
     */
    SettlementDecision decide(SettlementTransfer transfer);
}
