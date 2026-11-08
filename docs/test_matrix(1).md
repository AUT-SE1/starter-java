# Test Matrix

Levels: `unit` · `integration` · `contract` · `component` · `e2e`

## Backend

| # | Scenario | Input | Expected result | Level | Test file | Status |
|---|---|---|---|---|---|---|
| 1 | Process a valid RTGS transaction | `{"amount": 50000, "sourceIban": "IR...", ...}` | `200`, status is `COMPLETED` in DB | integration | `SettlementProcessingFlowTest.java` | to do |
| 2 | Amount below minimum (10,000) | `amount: 5000` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 3 | Amount above maximum (500M) | `amount: 600000000` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 4 | Invalid source IBAN format | `sourceIban: "12345"` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 5 | Required fields missing | `{"amount": null}` | `400`, Validation error | unit | `RtgsValidationServiceTest.java` | to do |
| 6 | Get settlement reports by valid date | `GET /api/v1/settlement-reports?date=2026-10-20` | `200`, returns report matching OpenAPI schema | integration | `SettlementReportApiTest.java` | done |
| 7 | Invalid report date format | `GET /api/v1/settlement-reports?date=invalid-date` | `400`, Bad Request | integration | `SettlementReportApiTest.java` | done |
| 8 | Calculate settlement summary metrics correctly | List of SUCCESS and FAILED transfers | Correct count, total amount, and success/failed split | unit | `SettlementReportServiceTest.java` | done |
| 9 | Fallback to zero values when no transfers found | `date=2026-10-20` with empty DB results | Zero count, zero amounts, empty items list | unit | `SettlementReportServiceTest.java` | done |
| 10 | Preserved correlation and causation tracking IDs | Inquiry command message IDs (`UUID`) | Output report preserves exact correlation and causation IDs | unit | `SettlementReportServiceTest.java` | done |
| 11 | Deterministic report ID generation across dates | Two different business dates | Generates unique deterministic UUIDs per business date | unit | `SettlementReportServiceTest.java` | done |
| 12 | Real PostgreSQL aggregation with status filter | Persisted transfers with status `SETTLED` | Query filters strictly by settledAt and status | integration | `SettlementReportIntegrationTest.java` | done |
| 13 | Date filter isolation in PostgreSQL | Transfers persisted for today vs tomorrow | Returns only transactions falling within the exact business date | integration | `SettlementReportIntegrationTest.java` | done |
| 14 | S6 Kafka producer publishes daily report | Valid `SettlementDailyReportEvent` | Published to topic with report ID key | unit | `S6KafkaComponentsTest.java` | done |
| 15 | S6 Kafka consumer processes inquiry command | Valid `ReportInquiryCommand` | Triggers service report generation and publishes reply | unit | `S6KafkaComponentsTest.java` | done |
| 16 | S6 Kafka consumer ignores inquiry with null date | `ReportInquiryCommand` with `business_date = null` | Inquiry safely skipped, no service call or reply emitted | unit | `S6KafkaComponentsTest.java` | done |
| 17 | S3 webhook (callbacks) valid call | a valid settlement result | schema matches S3's `openapi.yaml` | contract | `NeighborContractTest.java` | to do |
| 18 | S3 webhook (callbacks) validation | invalid settlement result | `400` and a Problem Details Error body | contract | `NeighborContractTest.java` | to do |
| 19 | ExecuteExternalTransfer DTO Serialization | valid Java object | accurate JSON representation | unit | `ExecuteExternalTransferTest.java` | done |
| 20 | Persist and retrieve SettlementTransfer | entity valid fields | saved in Postgres, fetched accurately | integration | `SettlementTransferRepositoryTest.java`| done |
| 21 | Save RTGS Transfer without source IBAN | entity with `sourceIban: null` | saved in Postgres without constraint violation | integration | `SettlementTransferRepositoryTest.java`| done |

## Frontend

| # | Scenario | Input | Expected result | Level | Test file | Status |
|---|---|---|---|---|---|---|
| 22 | Settlement Dashboard renders report metrics | API returns HTTP 200 payload | Metrics cards (total, success, failed) and container rendered | component | `SettlementDashboard.test.tsx` | done |
| 23 | Settlement Dashboard displays API 400 error | API returns 400 Bad Request | Error alert banner is displayed to user | component | `SettlementDashboard.test.tsx` | done |
| 24 | The RTGS form renders its fields | — | amount and IBAN inputs present | component | `RtgsForm.test.tsx` | to do |
| 25 | The form rejects invalid IBAN | submit with wrong IBAN format | an error message is shown (Problem Details) | component | `RtgsForm.test.tsx` | to do |
| 26 | A full RTGS transaction journey | fill the form correctly, submit | success message or updated status on screen | e2e | `RtgsJourney.spec.ts` | to do |

## Edge cases we deliberately did not cover

| Case | Why not, and when we will |
|---|---|
| S6 Kafka total broker outage / Retry exhaustion | Simulating 3 consecutive connection timeouts with backoff slows down local test execution; covered in end-to-end resilience tests. |
| Scheduled Paya processing | Out of scope for this sprint based on the User Story. Will be addressed in a future task. |
| Database connection failure during processing | Cannot be easily simulated in standard unit tests without heavy mocking. We rely on standard `@Transactional` rollbacks. |