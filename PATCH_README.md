# Patch: RTGS settlement engine + build blockers (S5)

محتوای این zip را **روی ریشه‌ی ریپو** (کنار pom.xml) کپی کنید؛ مسیرها دقیقاً مثل ریپو است.
فایل‌های هم‌نام جایگزین می‌شوند.

## الف) فایل‌های جدید
| مسیر | کار |
|---|---|
| src/main/resources/db/migration/V2__rtgs_engine.sql | ستون‌ها و constraintهای جدید (RTGS، attempt_count، failure_code، source_iban nullable) |
| src/main/java/.../entity/SettlementStatus.java, SettlementType.java | ثابت‌های وضعیت و نوع |
| src/main/java/.../engine/* (4 فایل) | interface موتور، تصمیم، خطای موقت، RtgsSettlementEngine |
| src/main/java/.../config/SettlementEngineConfig.java | Clock و RandomGenerator |
| src/main/java/.../config/KafkaClustersConfig.java | producer S3، listener factoryهای S3 و S6 |
| src/main/java/.../service/* (5 فایل) | state، processing، ingestion، event، trigger |
| src/test/java/.../PostgresIntegrationTest.java | پایه‌ی تست با Postgres واقعی (Testcontainers) |
| src/test/java/.../engine/RtgsSettlementEngineTest.java | تست واحد موتور |
| src/test/java/.../SettlementProcessingFlowTest.java | تست سناریوهای معیار پذیرش |

## ب) فایل‌هایی که جایگزین می‌شوند
entity/SettlementTransfer.java, repository/SettlementTransferRepository.java,
dto/ExecuteExternalTransfer.java, dto/ExecuteExternalTransferResult.java (به record تبدیل شدند),
consumer/SettlementKafkaConsumer.java, consumer/ReportResultConsumer.java, consumer/ReportInquiryConsumer.java,
producer/SettlementKafkaProducer.java, application.properties, docker-compose.yml, .env.example,
test/.../SettlementTransferRepositoryTest.java

## ج) حذف کنید
- src/main/java/edu/softwarelab/starter/kafka/KafkaTopics.java  (کلاس خالی؛ پوشه‌ی kafka/ هم پاک شود)
- src/test/java/CalculatorTest.java

## د) ویرایش دستی کوچک

1) **Dockerfile**: `EXPOSE 8080` → `EXPOSE 8085`

2) **src/test/java/edu/softwarelab/starter/OrderApiTest.java** (و هر تست @SpringBootTest دیگر):
   `class OrderApiTest {`  →  `class OrderApiTest extends PostgresIntegrationTest {`
   (چون context حالا Flyway/DB می‌خواهد.)

3) **service/SettlementReportService.java** (هماهنگی گزارش با وضعیت‌های جدید):
   - `FINALIZED_STATUSES = Set.of("SUCCESS", "FAILED")`
     →  `Set.of("SETTLED", "SETTLED_CONFIRMED", "FAILED")`
   - داخل `mapToReportItem` قبل از `return`:
     `String status = Set.of("SETTLED", "SETTLED_CONFIRMED").contains(transfer.getStatus()) ? "SUCCESS" : transfer.getStatus();`
     و در `new SettlementReportItem(...)` به‌جای `transfer.getStatus(),` بنویسید `status,`
   - داخل `processReportResult`: `"SUCCESS", "SETTLED_CONFIRMED"` → `"SETTLED", "SETTLED_CONFIRMED"`

## ه) اجرا
```bash
cp .env.example .env            # DB_PASSWORD را پر کنید
docker compose up -d postgres kafka
export DB_PASSWORD=...          # همان مقدار .env ؛ برای اجرای mvn روی سیستم خودتان
mvn clean verify                # تست‌ها به Docker روشن نیاز دارند
mvn spring-boot:run             # یا: docker compose up --build
```

## و) تصمیم‌هایی که باید در ADR ثبت و با S3 هماهنگ شود
1. RTGS به‌صورت مقدار جدید `RTGS` در DB آمده (نه alias برای SATNA).
2. پیام ExecuteExternalTransfer نوع تسویه ندارد؛ `settlement.default-type=RTGS` است.
3. `transaction_id` پیام = id ردیف و `operation_id` = idempotency_key.
4. نسخه‌ی تاپیک (`...v2`) با پیام v3 مغایرت دارد؛ تأیید شود.
