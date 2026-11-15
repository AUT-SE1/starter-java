# Session 06 Bug Fix Report

**Service:** S5 Settlement Reporting Service  
**Team members:** سید حامد میردهقان; محمد امین آگاهی فرد; امیرحسین شوری زاده  
**Exercise repository:** https://github.com/AUT-SE1/LabBank-S5-Session06.git  
**Team repository:** https://github.com/Amirhosein-shoorizadeh/starter-java-lab.git

## Incorrect Behavior and Root Cause

`GET /api/v1/settlement-reports?date=YYYY-MM-DD` omitted `FAILED` transfers. `FAILED` was missing from `FINALIZED_STATUSES`, so the service did not include it in the statuses sent to the repository. The repository filtered records by those statuses, removing failed transfers before summary calculation.

## Fix

Added `FAILED` to `FINALIZED_STATUSES` and centralized successful statuses in `SUCCESS_STATUSES`. Summary calculation now includes failed transfers in the report while adding only successful transfer amounts to `totalAmount`.

## Regression Test and Results

`testDailyReportIncludesSettledAndFailedTransfers` stores a `SETTLED` transfer of 350,000 and a `FAILED` transfer, then checks both transactions and the summary:

- `totalCount = 2`
- `successCount = 1`
- `failedCount = 1`
- `totalAmount = 350000`

Before the fix, the test failed because only the `SETTLED` transaction appeared. After the fix, it passed successfully.

JDB observations also confirmed the behavior: before the fix, the repository returned only one `SETTLED` record. After the fix, both `SETTLED` and `FAILED` transfers reached summary calculation, producing the expected results.

All 14 Session 06 related tests passed with no failures or errors.

## Files Changed

For the reporting bug: `SettlementReportService.java` and `SettlementReportIntegrationTest.java`.

Pre-existing exercise setup changes also present in the worktree: `Dockerfile` and `pom.xml` (runtime image and Testcontainers setup). No `.env` content or credentials are included.

## Final Result

The daily settlement report bug was successfully fixed. The regression test and all 14 Session 06 related tests passed, confirming that failed transfers are now included in the report while `totalAmount` continues to count only successful transfers.
