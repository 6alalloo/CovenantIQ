# Loan Import Test Implementation Backlog

## Purpose

This backlog turns the missing loan import test cases from [loan_import_test_case_matrix.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_test_case_matrix.md) into concrete implementation work.

It is intended to answer three questions for each missing area:
- which test layer should own it
- which file should contain it
- what test method or spec name should be added

## Priority Model

- `P1`: critical for feature confidence and basic release readiness
- `P2`: important for business-rule correctness and regression protection
- `P3`: useful completeness and UI/documentation confidence

## Backend Service Tests

These do not exist yet, but the implementation plan already expects them.

### Proposed File: `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java`

Add these first:

| Priority | Test Case IDs | Proposed Test Name | Why |
| --- | --- | --- | --- |
| P1 | LI-021 | `createsLoanWhenNoExistingMatchIsFound()` | Validates core create path from `sourceSystem + externalLoanId`. |
| P1 | LI-021 | `updatesExistingLoanWhenMatchingSourceSystemAndExternalLoanIdExists()` | Validates the core update path for repeated imports. |
| P1 | LI-022 | `preservesUserManagedFieldsWhenUpdatingImportedLoan()` | Protects the ownership boundary. |
| P1 | LI-023 | `closesActiveLoanWhenImportedStatusIsClosed()` | Verifies the allowed status transition. |
| P1 | LI-024 | `rejectsClosedToActiveReopenWhenReopenIsNotAllowed()` | Verifies the planned v1 business rule. |
| P2 | LI-020 | `returnsUnchangedWhenImportedRowHasNoEffectiveFieldChanges()` | Covers idempotency and duplicate-safe re-import. |
| P2 | LI-022 | `doesNotDeleteOrReplaceMonitoringArtifactsDuringImportUpdate()` | Ensures covenants, alerts, comments, and related child data remain intact. |
| P2 | LI-021 | `matchesOnlyBySourceSystemAndExternalLoanId()` | Prevents accidental matching by borrower or amount. |
| P3 | LI-030 | `setsSyncMetadataOnImportedLoan()` | Verifies `lastSyncedAt`, `sourceUpdatedAt`, and imported markers are populated. |

### Proposed File: `src/test/java/com/covenantiq/service/LoanImportServiceTest.java`

Add these next:

| Priority | Test Case IDs | Proposed Test Name | Why |
| --- | --- | --- | --- |
| P1 | LI-015 | `previewRejectsMalformedCsv()` | Covers invalid CSV parse handling. |
| P1 | LI-016 | `previewRejectsEmptyFile()` | Covers file-level validation. |
| P1 | LI-017 | `previewRejectsMissingRequiredHeaders()` | Covers required-column validation. |
| P1 | LI-018 | `previewFlagsInvalidRowValues()` | Covers row-level validation for dates, enums, and amounts. |
| P1 | LI-019 | `previewFlagsDuplicateSourceSystemAndExternalLoanIdRows()` | Covers same-file duplicate detection. |
| P1 | LI-025 | `executeProcessesValidRowsAndReportsInvalidRows()` | Covers mixed-result batches. |
| P2 | LI-020 | `executeMarksRowsUnchangedWhenNoDataChangesExist()` | Covers unchanged-row reporting from the import orchestration layer. |
| P2 | LI-026 | `listBatchesReturnsImportHistoryInDescendingOrder()` | Gives real coverage to import history. |
| P2 | LI-027 | `getBatchDetailReturnsSummaryAndRowCounts()` | Covers batch detail retrieval. |
| P3 | LI-025 | `executeRejectsStaleOrNonPreviewReadyBatch()` | Covers execution-time batch state safety. |

## Backend Integration Tests

### Existing File To Extend
- [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java)

Add these integration tests before creating more files unless the class becomes unwieldy.

| Priority | Test Case IDs | Proposed Test Name | Why |
| --- | --- | --- | --- |
| P1 | LI-015 | `adminPreviewRejectsMalformedCsv()` | Confirms controller, parser, and response contract for bad files. |
| P1 | LI-017 | `adminPreviewRejectsMissingHeaders()` | Confirms API behavior for required header validation. |
| P1 | LI-025 | `adminCanExecuteBatchWithMixedValidAndInvalidRows()` | Confirms end-to-end batch accounting behavior. |
| P1 | LI-026 | `adminCanListLoanImportHistory()` | Confirms history endpoint behavior and auth. |
| P1 | LI-027 | `adminCanFetchLoanImportBatchDetail()` | Confirms batch detail endpoint behavior. |
| P2 | LI-020 | `reimportOfSameDataReturnsUnchangedAndDoesNotCreateDuplicateLoan()` | Confirms idempotent behavior end to end. |
| P2 | LI-021 | `reimportUpdatesExistingLoanInsteadOfCreatingAnotherLoan()` | Confirms update path end to end. |
| P2 | LI-024 | `previewOrExecuteRejectsForbiddenClosedToActiveReopen()` | Confirms business-rule enforcement through the API. |
| P2 | LI-022 | `importUpdateDoesNotAlterExistingMonitoringRecords()` | Confirms ownership rules with persisted related records. |
| P3 | LI-016 | `adminPreviewRejectsEmptyFile()` | Useful if file-level validation is enforced at controller/service boundary. |

## Frontend E2E Tests

### Existing File To Extend
- [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts)

Add these Playwright tests:

| Priority | Test Case IDs | Proposed Test Name | Why |
| --- | --- | --- | --- |
| P1 | LI-028 | `E2E-092 invalid loan import file shows validation errors` | Covers the main missing UX validation scenario. |
| P1 | LI-029 | `E2E-093 admin can review prior loan import history` | Covers the import history panel in the UI. |
| P2 | LI-020 | `E2E-094 unchanged loan import row is shown as UNCHANGED` | Covers visible idempotency behavior. |
| P2 | LI-025 | `E2E-095 mixed-result import shows created updated unchanged and failed counts` | Covers summary rendering for non-happy-path import results. |
| P2 | LI-030 | `E2E-096 imported loan overview shows external metadata` | Covers imported-loan visibility in the loan UI. |
| P3 | LI-017 | `E2E-097 missing required columns disables import and shows file-level error` | Covers stricter upload validation if exposed in the UI. |

## Suggested Delivery Order

### Sprint 1: Minimum Release Confidence

Implement first:
- `LoanImportServiceTest.previewRejectsMalformedCsv()`
- `LoanImportServiceTest.previewRejectsMissingRequiredHeaders()`
- `LoanImportServiceTest.executeProcessesValidRowsAndReportsInvalidRows()`
- `ExternalLoanSyncServiceTest.updatesExistingLoanWhenMatchingSourceSystemAndExternalLoanIdExists()`
- `ExternalLoanSyncServiceTest.preservesUserManagedFieldsWhenUpdatingImportedLoan()`
- `ExternalLoanSyncServiceTest.rejectsClosedToActiveReopenWhenReopenIsNotAllowed()`
- `LoanImportIntegrationTest.adminCanListLoanImportHistory()`
- `loan-imports.spec.ts` `E2E-092 invalid loan import file shows validation errors`

### Sprint 2: Business Rule Completion

Implement next:
- unchanged-row tests at service, integration, and UI layers
- close-status transition test
- batch detail retrieval test
- mixed-result UI summary test
- imported metadata visibility test

### Sprint 3: Completeness And Hardening

Implement last:
- empty-file variations
- stricter history ordering assertions
- stale preview batch handling
- explicit same-file duplicate row UI presentation if the page renders it distinctly

## Ownership Recommendation

- Service-layer tests should own business rules and field ownership logic.
- Integration tests should own endpoint contracts, security, and persistence interactions.
- Playwright tests should own route protection, validation rendering, and summary/history visibility.

## Immediate Next Files To Create

Create these first if you want the backlog converted into code work:
- `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java`
- `src/test/java/com/covenantiq/service/LoanImportServiceTest.java`

Then extend these existing files:
- [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java)
- [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts)

## Exit Criteria For Stronger Coverage

You can consider loan import testing materially stronger once these are true:
- create, update, unchanged, and mixed-result flows are all covered
- file-level and row-level validation failures are covered
- role restrictions are covered in both API and UI
- ownership rules are covered so imported data does not overwrite monitoring artifacts
- history and batch detail retrieval are covered
- imported metadata visibility is covered in the frontend
