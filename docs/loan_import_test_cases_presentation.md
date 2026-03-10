# Loan Import Test Cases

Prepared for CovenantIQ

Date: March 10, 2026
Feature: Admin Loan Imports
Scope: Backend services, backend integration, and frontend E2E coverage for the CSV-driven loan import flow

## Executive Summary

This document presents the current automated test cases for the CovenantIQ loan import feature.

Current status as of March 10, 2026:
- Covered: 30 test case areas
- Partial: 0 test case areas
- Missing: 0 test case areas

Validation completed for the current implementation:
- Backend targeted verification: `mvn -q "-Dtest=LoanImportServiceTest,LoanImportIntegrationTest,ExternalLoanSyncServiceTest" test`
- Frontend targeted verification: `$env:SPRING_PROFILES_ACTIVE='test'; cmd /c npx playwright test tests/e2e/loan-imports.spec.ts --workers=1`

## Coverage Summary

| Layer | Coverage |
| --- | --- |
| Backend service tests | Field ownership, create, update, unchanged, close, reopen rejection, row validation, and empty-file handling |
| Backend integration tests | Preview, execute, mixed-result batches, history, batch detail, reimport unchanged, reimport update, empty-file rejection, row-level validation, reopen rejection, and admin-only access |
| Frontend E2E tests | Admin access, invalid file handling, preview/execute flow, history visibility, unchanged row visibility, row-level validation visibility, reopen rejection visibility, and imported metadata visibility |

## Current Test Cases

All `LI-001` through `LI-030` are currently covered by automated tests.

## Notes On Recent Closures

The following previously open or partial areas are now covered:
- `LI-016`: empty file rejection is covered in service and integration tests.
- `LI-018`: invalid row values are now surfaced as preview row errors instead of aborting the whole file.
- `LI-024`: closed-to-active reopen rejection is covered in backend integration and frontend E2E.

## Verification References

Backend service tests:
- `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java`
- `src/test/java/com/covenantiq/service/LoanImportServiceTest.java`

Backend integration tests:
- `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java`

Frontend E2E tests:
- `frontend/tests/e2e/loan-imports.spec.ts`

Supporting implementation surface:
- `src/main/java/com/covenantiq/service/CsvLoanImportParser.java`
- `src/main/java/com/covenantiq/service/LoanImportService.java`
- `src/main/java/com/covenantiq/service/ExternalLoanSyncService.java`
- `frontend/src/pages/AdminLoanImportsPage.tsx`
- `frontend/src/pages/LoanOverviewPage.tsx`

## Recommendation

The loan import feature now has complete automated coverage across the current planned test case set, including the previously partial empty-file handling, row-level invalid-value preview handling, and forbidden reopen behavior.
