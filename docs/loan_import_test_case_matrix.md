# Loan Import Test Case Matrix

## Purpose

This document converts the current loan import automation into a usable test case matrix and identifies the missing scenarios required for fuller coverage.

It is based on:
- the implementation targets in [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md)
- the current backend automation in [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java)
- the current frontend automation in [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts)

## Status Summary

- This repo already has enough detail to produce a first-pass loan import test case document.
- Current loan import coverage is strong on happy path and access control.
- Current loan import coverage is incomplete for validation edge cases, business rule enforcement, idempotency, and history/detail retrieval depth.

## How To Read This

- `Covered`: there is an existing automated test that exercises the scenario directly.
- `Partial`: related behavior exists, but the exact business rule is not fully asserted.
- `Missing`: no dedicated automated test was found for the scenario.

## Current Automated Test Cases

| ID | Scenario | Layer | Status | Evidence |
| --- | --- | --- | --- | --- |
| LI-001 | Admin can open loan imports page | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):4 |
| LI-002 | Admin can upload a valid CSV and see preview content | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):10 |
| LI-003 | Run Import button becomes available after valid preview | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):15 |
| LI-004 | Admin can execute a loan import from the UI | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):17 |
| LI-005 | Completed batch status is shown after successful import | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):23 |
| LI-006 | Analyst cannot access the admin loan imports route | Frontend E2E | Covered | [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):27 |
| LI-007 | Admin can preview a valid loan import file via API | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):32 |
| LI-008 | Preview result is marked `PREVIEW_READY` | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):44 |
| LI-009 | Preview marks a new row as `CREATE` | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):49 |
| LI-010 | Admin can execute a previewed batch via API | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):57 |
| LI-011 | Executed import creates a loan record visible in the loan list | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):65 |
| LI-012 | Admin can retrieve batch row results after execution | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):72 |
| LI-013 | Analyst cannot preview loan imports via API | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):79 |
| LI-014 | Analyst cannot list loan imports via API | Backend integration | Covered | [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):97 |

## Planned Test Cases Not Yet Covered

These scenarios are either explicitly called for in the implementation plan or are necessary to turn the current matrix into a more complete formal test case set.

| ID | Scenario | Expected Result | Status | Source |
| --- | --- | --- | --- | --- |
| LI-015 | Preview rejects invalid CSV structure | Invalid file is rejected with validation feedback | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):574 |
| LI-016 | Preview rejects empty file | File-level validation error is returned | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):376 |
| LI-017 | Preview rejects missing required headers | Missing headers are called out clearly | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):376 |
| LI-018 | Preview flags invalid row values | Invalid date, enum, or amount errors are shown per row | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):384 |
| LI-019 | Preview flags duplicate `sourceSystem + externalLoanId` rows in same file | Duplicate row is marked as invalid | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):394 |
| LI-020 | Re-import with unchanged data returns `UNCHANGED` | Duplicate entity creation does not occur | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):94 |
| LI-021 | Import updates an existing loan matched by `sourceSystem + externalLoanId` | Existing loan master fields are updated | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):64 |
| LI-022 | Import preserves user-managed monitoring records | Covenants, alerts, comments, attachments, and related child records are not overwritten | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):57 |
| LI-023 | Import closes an active loan when status is `CLOSED` | Loan status updates to `CLOSED` and child history remains intact | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):80 |
| LI-024 | Import rejects automatic `CLOSED -> ACTIVE` reopen | Row is rejected and validation error is recorded | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):85 |
| LI-025 | Import executes valid rows while invalid rows are reported | Mixed-result batch completes with correct counts | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):577 |
| LI-026 | Import history list can be retrieved by admin | Batch list endpoint returns prior imports with totals and status | Partial | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):579 |
| LI-027 | Single import batch detail can be retrieved by admin | Batch detail contains summary metadata | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):322 |
| LI-028 | Frontend shows validation errors for invalid file | User sees row-level or file-level error feedback before import | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):589 |
| LI-029 | Frontend shows prior import history | Admin can review prior imports in the page | Partial | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):196 |
| LI-030 | Imported loan metadata is visible in loan views | Source system, external ID, and sync timestamps are shown | Missing | [loan_import_implementation_plan.md](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/docs/loan_import_implementation_plan.md):510 |

## Suggested Formal Test Case Format

For a business-facing or QA-facing test case document, each case should be expanded into:
- Test case ID
- Title
- Objective
- Preconditions
- Test data
- Steps
- Expected result
- Automation reference
- Coverage status

## Example Expanded Cases

### LI-007 Admin can preview a valid loan import file via API

- Objective: Confirm that an admin can upload a valid CSV and receive a preview without changing persisted loan data.
- Preconditions: Admin user exists and is authenticated.
- Test data: A CSV with valid `sourceSystem`, `externalLoanId`, `borrowerName`, `principalAmount`, `startDate`, `status`, and optional `sourceUpdatedAt`.
- Steps:
1. Authenticate as admin.
2. Submit the CSV to `POST /api/v1/admin/loan-imports/preview`.
3. Inspect the response payload.
- Expected result:
1. Response status is `200 OK`.
2. Batch status is `PREVIEW_READY`.
3. First row action is `CREATE`.
- Automation reference: [LoanImportIntegrationTest.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java):32

### LI-006 Analyst cannot access the admin loan imports route

- Objective: Confirm that non-admin users cannot use the admin import UI.
- Preconditions: Analyst user exists and is authenticated.
- Test data: None beyond an analyst login.
- Steps:
1. Login as analyst.
2. Navigate to `/app/admin/loan-imports`.
- Expected result:
1. User is redirected to `/forbidden`.
2. Access forbidden message is shown.
- Automation reference: [loan-imports.spec.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/tests/e2e/loan-imports.spec.ts):27

## Coverage Assessment

You can use this document today as:
- a first-pass test case document for the loan import feature
- a traceability matrix between implementation intent and current automation
- a backlog for missing automated tests

You should not use it yet as:
- a complete UAT script pack
- a claim of full loan import coverage
- a final QA sign-off artifact

## Recommended Next Additions

If the goal is a stronger formal test case package, add these next:
- backend tests for invalid CSV preview, update path, unchanged path, duplicate rows, forbidden reopen, and mixed valid/invalid execution
- frontend E2E for invalid file handling and import history display
- service-level tests for field ownership preservation and matching logic
