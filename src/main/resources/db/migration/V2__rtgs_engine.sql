-- V2: ستون‌ها و constraintهای لازم برای موتور تسویه‌ی RTGS
-- (به V1 دست نزنید؛ Flyway checksum آن را چک می‌کند)

ALTER TABLE settlement_transfers
    ADD COLUMN attempt_count          INT NOT NULL DEFAULT 0,
    ADD COLUMN processing_started_at  TIMESTAMPTZ,
    ADD COLUMN failure_code           VARCHAR(50);

-- پیام ExecuteExternalTransfer از S3 شبای مبدأ نمی‌فرستد
ALTER TABLE settlement_transfers ALTER COLUMN source_iban DROP NOT NULL;

-- نوع RTGS اضافه می‌شود
ALTER TABLE settlement_transfers DROP CONSTRAINT chk_settlement_type;
ALTER TABLE settlement_transfers
    ADD CONSTRAINT chk_settlement_type CHECK (settlement_type IN ('SATNA', 'PAYA', 'RTGS'));

-- SETTLED_CONFIRMED بعد از تأیید S6 روی گزارش روزانه ثبت می‌شود
ALTER TABLE settlement_transfers DROP CONSTRAINT chk_settlement_status;
ALTER TABLE settlement_transfers
    ADD CONSTRAINT chk_settlement_status
    CHECK (status IN ('ACCEPTED', 'PROCESSING', 'SETTLED', 'FAILED', 'SETTLED_CONFIRMED'));

CREATE INDEX idx_settlement_transfers_status_processing
    ON settlement_transfers (status, processing_started_at);
