# Test Matrix

Fill this in during Session 05. Every row must map to a real test file — a matrix with
no tests behind it is a wiki page, not a test plan.

Levels: `unit` · `integration` · `contract` · `e2e`

## Backend

| # | Scenario | Input | Expected result | Level | Test file | Status |
|---|---|---|---|---|---|---|
| 1 | Process a valid RTGS transaction | `{"amount": 50000, "sourceIban": "IR...", ...}` | `200`, status is `COMPLETED` in DB | integration | `SettlementProcessingFlowTest.java` | to do |
| 2 | Amount below minimum (10,000) | `amount: 5000` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 3 | Amount above maximum (500M) | `amount: 600000000` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 4 | Invalid source IBAN format | `sourceIban: "12345"` | `400`, throws IllegalArgumentException | unit | `RtgsValidationServiceTest.java` | to do |
| 5 | Required fields missing | `{"amount": null}` | `400`, Validation error | unit | `RtgsValidationServiceTest.java` | to do |
| 6 | Get settlement reports by date | `GET /api/v1/settlement-reports?date=2025-11-15` | `200`, returns list of reports | integration | `SettlementReportControllerTest.java` | to do |
| 7 | Unknown report date format | `GET /api/v1/settlement-reports?date=invalid` | `400`, Bad Request | integration | `SettlementReportControllerTest.java` | to do |
| 8 | S3 webhook (callbacks) valid call | a valid settlement result | schema matches S3's `openapi.yaml` | contract | `NeighborContractTest.java` | to do |
| 9 | S3 webhook (callbacks) validation | invalid settlement result | `400` and a Problem Details Error body | contract | `NeighborContractTest.java` | to do |

## Frontend

| # | Scenario | Input | Expected result | Level | Test file | Status |
|---|---|---|---|---|---|---|
| 10 | The RTGS form renders its fields | — | amount and IBAN inputs present | component | `RtgsForm.test.tsx` | to do |
| 11 | The form rejects invalid IBAN | submit with wrong IBAN format | an error message is shown (Problem Details) | component | `RtgsForm.test.tsx` | to do |
| 12 | A full RTGS transaction journey | fill the form correctly, submit | success message or updated status on screen | e2e | `RtgsJourney.spec.ts` | to do |

## Edge cases we deliberately did not cover

| Case | Why not, and when we will |
|---|---|
| Kafka message production | Out of scope for this sprint based on the User Story requirements. Will implement when Kafka integration is required. |
| Scheduled Paya processing | Out of scope for this sprint based on the User Story. Will be addressed in a future task. |
| Database connection failure during processing | Cannot be easily simulated in standard unit tests without heavy mocking. We rely on standard `@Transactional` rollbacks. |