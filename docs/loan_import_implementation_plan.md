# Loan Import Implementation Plan

## Purpose

This document describes how to add an admin-only external loan import flow to CovenantIQ.

The goal is to let externally sourced loan master data enter the system through a controlled import experience, while keeping user-managed monitoring data inside CovenantIQ.

This plan assumes the ownership boundary we agreed on:
- external source owns loan master data
- CovenantIQ users own covenants, financial statements, alerts, comments, attachments, workflows, rulesets, exceptions, and change control

## Why This Is The Right Model

The app already treats loans as the parent record for downstream monitoring.

That means loan master data can be updated from outside the app, while the monitoring and analyst workflow layers remain locally managed.

This keeps the boundary clean:
- imported data creates or updates the loan shell
- users add monitoring logic and operational context inside CovenantIQ
- external updates never overwrite analyst-managed monitoring records

## Scope

### In Scope
- admin-only import UI for loan master data
- backend import endpoint(s)
- validation and preview flow
- create/update behavior for loan master fields
- import audit history
- sync metadata stored on loans
- optional manual re-import of the same file
- bulk import from CSV first

### Out Of Scope
- automated polling from an external system
- inbound webhooks from external systems
- import of covenants from external systems
- import of financial statements from external systems through this feature
- real-time push to the UI using SSE or WebSockets
- deletion sync from external systems

## Business Rules

### Ownership Rules
External import may create or update only these loan master fields:
- external loan identifier
- source system name
- borrower name
- principal amount
- start date
- loan status
- source-provided last-updated timestamp
- local last-synced timestamp

External import must not create, update, or delete:
- covenants
- financial statements
- covenant results
- alerts
- comments
- attachments
- collateral
- covenant exceptions
- workflow state or definitions
- rulesets or ruleset versions
- change requests or releases

### Matching Rules
Loans are matched by:
- `sourceSystem + externalLoanId`

Loans must never be matched by:
- borrower name
- principal amount
- start date

### Status Rules
If the external import marks a loan as `CLOSED`:
- update the local loan status to `CLOSED`
- keep all historical monitoring data intact
- do not delete any child records

If the external import marks a previously closed loan as `ACTIVE`:
- allow reopen only if the product owner wants that behavior
- otherwise reject the row and log a validation error

Recommended first release:
- allow `ACTIVE -> CLOSED`
- reject automatic `CLOSED -> ACTIVE` unless explicitly approved

### Idempotency Rules
Repeated imports of the same row should be safe.

If the same row is imported again with no field changes:
- do not create duplicate loans
- do not create duplicate audit records at the entity level unless needed
- mark the row as `UNCHANGED` in import results if possible

## User Experience

## User Persona
This flow is for:
- admins
- technical operators
- demo operators importing externally sourced loan lists

This flow is not for:
- analysts doing normal day-to-day monitoring work

## UX Model
Use Model B: Admin Import UI.

Add a new admin-accessible page for bulk loan imports.

Suggested route:
- `/app/admin/loan-imports`

Alternative if you want it grouped under settings:
- `/app/settings/loan-imports`

Recommended choice:
- `/app/admin/loan-imports`

That is consistent with this being a controlled admin function rather than a general user feature.

## Page Goals
The page should let an admin:
- upload a CSV file
- preview parsed rows before commit
- see row-level validation results
- confirm import
- review import results
- review prior imports

## UI Sections

### 1. Intro Block
Explain clearly:
- this page is for importing externally managed loan master data
- imported data updates loan shell records only
- monitoring configuration remains user-managed in CovenantIQ

### 2. File Upload Block
Allow upload of a CSV file.

Show:
- accepted file type
- required columns
- downloadable example format

Recommended required columns:
- `sourceSystem`
- `externalLoanId`
- `borrowerName`
- `principalAmount`
- `startDate`
- `status`

Recommended optional columns:
- `sourceUpdatedAt`

### 3. Preview Table
Before final import, show each parsed row with:
- row number
- external loan ID
- borrower name
- detected action: `CREATE`, `UPDATE`, `UNCHANGED`, `ERROR`
- validation issues if any

### 4. Import Summary
Show:
- total rows
- valid rows
- invalid rows
- creates
- updates
- unchanged rows

### 5. Confirm Import Action
Only enabled if preview succeeds.

Button text:
- `Run Import`

### 6. Results Panel
After import, show:
- rows created
- rows updated
- rows unchanged
- rows failed
- downloadable error report if useful

### 7. Import History
Show recent imports with:
- import ID
- file name
- uploaded by
- started at
- finished at
- status
- totals
- link to detailed results

## Backend Design

## New Data Model

### 1. Extend Loan Entity
Update [Loan.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/domain/Loan.java) to add fields such as:
- `externalLoanId`
- `sourceSystem`
- `lastSyncedAt`
- `sourceUpdatedAt`
- `syncManaged`

Recommended semantics:
- `externalLoanId`: stable upstream record ID
- `sourceSystem`: upstream system name
- `lastSyncedAt`: when CovenantIQ last applied imported data
- `sourceUpdatedAt`: source system's last update timestamp
- `syncManaged`: whether the loan is externally managed

Recommended constraints:
- unique constraint on `sourceSystem + externalLoanId`
- nullable for legacy/manual loans

### 2. Add Import Audit Entities
Add new entities for import tracking.

Recommended:
- `LoanImportBatch`
- `LoanImportRow`

#### LoanImportBatch fields
- `id`
- `fileName`
- `uploadedBy`
- `startedAt`
- `completedAt`
- `status`
- `totalRows`
- `validRows`
- `invalidRows`
- `createdCount`
- `updatedCount`
- `unchangedCount`
- `failedCount`
- `sourceSystem` or mixed-source flag

#### LoanImportRow fields
- `id`
- `batchId`
- `rowNumber`
- `externalLoanId`
- `borrowerName`
- `action` (`CREATE`, `UPDATE`, `UNCHANGED`, `ERROR`)
- `validationMessage`
- `loanId` nullable
- `rawPayloadJson`

This gives you proper auditability and admin troubleshooting.

## Repositories
Add:
- `LoanImportBatchRepository`
- `LoanImportRowRepository`

Update [LoanRepository.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/repository/LoanRepository.java) to include:
- `Optional<Loan> findBySourceSystemAndExternalLoanId(String sourceSystem, String externalLoanId)`

## Service Layer

### 1. ExternalLoanSyncService
Create:
- `src/main/java/com/covenantiq/service/ExternalLoanSyncService.java`

Responsibilities:
- normalize imported loan row data
- find existing loan by `sourceSystem + externalLoanId`
- create loan if no match exists
- update only external-owned fields if match exists
- apply status rules
- set sync metadata
- return row outcome

This service should not parse CSV files directly. Keep parsing separate.

### 2. LoanImportService
Create:
- `src/main/java/com/covenantiq/service/LoanImportService.java`

Responsibilities:
- receive uploaded CSV
- parse and validate rows
- produce preview result
- persist import batch metadata
- run row-by-row sync using `ExternalLoanSyncService`
- persist row results
- return summary response

### 3. CsvLoanImportParser
Create a dedicated parser component such as:
- `src/main/java/com/covenantiq/service/CsvLoanImportParser.java`

Responsibilities:
- read CSV
- map columns to DTOs
- validate required headers
- return normalized row DTOs

Do not put CSV parsing logic directly in controllers.

## API Design

## Endpoints
Add new admin-only endpoints under a distinct namespace.

Recommended:
- `POST /api/v1/admin/loan-imports/preview`
- `POST /api/v1/admin/loan-imports`
- `GET /api/v1/admin/loan-imports`
- `GET /api/v1/admin/loan-imports/{id}`
- `GET /api/v1/admin/loan-imports/{id}/rows`

Why this route shape is better than `/external/**` for Model B:
- this is a human-admin import feature, not a machine-to-machine API
- it belongs with admin operations
- auth and auditing map naturally to admin users

If you later add true machine-to-machine sync, keep that separate under `/api/v1/external/**`.

## Preview Endpoint
Input:
- multipart upload of CSV file

Behavior:
- parse file
- validate rows
- infer action per row
- do not modify loans yet
- optionally persist a temporary or durable preview batch record

Output:
- summary counts
- row outcomes
- normalized preview rows

## Import Execute Endpoint
Input:
- either re-upload file or reference a preview batch ID

Recommended choice:
- preview creates a persisted draft batch
- execute imports by batch ID

That avoids reparsing mismatched files and makes auditing easier.

Behavior:
- execute valid rows only
- record row outcomes
- mark batch complete

## Security

Update [SecurityConfig.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/config/SecurityConfig.java) to protect these endpoints as admin-only.

Recommended access rule:
- `hasRole('ADMIN')`

The page itself should also be admin-only in the frontend route tree.

## Validation Rules

## File-Level Validation
Reject the file if:
- required headers are missing
- file is empty
- file cannot be parsed as CSV
- file exceeds size limits

## Row-Level Validation
Validate per row:
- `sourceSystem` is present
- `externalLoanId` is present
- `borrowerName` is present
- `principalAmount` is a positive decimal
- `startDate` is a valid date
- `status` is a valid supported enum
- `sourceUpdatedAt` is valid if provided

## Business Validation
- duplicate `sourceSystem + externalLoanId` rows in the same file should be flagged
- rows attempting forbidden state changes should be flagged
- rows with no effective change should be marked `UNCHANGED`

## Mapping Rules

CSV to domain mapping should be explicit.

Example:
- `sourceSystem` -> `Loan.sourceSystem`
- `externalLoanId` -> `Loan.externalLoanId`
- `borrowerName` -> `Loan.borrowerName`
- `principalAmount` -> `Loan.principalAmount`
- `startDate` -> `Loan.startDate`
- `status` -> `Loan.status`
- `sourceUpdatedAt` -> `Loan.sourceUpdatedAt`
- import timestamp -> `Loan.lastSyncedAt`
- imported record -> `Loan.syncManaged = true`

## Interaction With Existing Loan Logic

Current loan creation is handled in [LoanService.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/service/LoanService.java).

Do not overload the existing `createLoan(CreateLoanRequest)` method for imported data.

Reason:
- that method is user-driven
- it assumes local creation semantics
- it emits activity and outbox events specific to manual creation

Instead:
- keep `LoanService` for manual user actions
- add `ExternalLoanSyncService` for imported loan master data

You may still reuse shared helper logic by extracting common field assignment helpers if needed.

## Activity Logging And Audit

Imported changes should show up in existing activity history where helpful.

Recommended events:
- `LOAN_IMPORTED`
- `LOAN_SYNC_UPDATED`
- `LOAN_SYNC_SKIPPED`

If adding new `ActivityEventType` values is too heavy for the first pass, at minimum log descriptive activity text.

Recommended audit detail examples:
- `Loan imported from CORE_BANKING`
- `Loan sync updated borrowerName and principalAmount from external source`
- `Loan sync skipped because no source-owned fields changed`

## Outbox Events

Optional but recommended:
- publish `LoanImported`
- publish `LoanUpdatedFromImport`

This fits your existing outbox pattern and keeps downstream integrations consistent.

However, do not emit noisy events for unchanged rows.

## Frontend Implementation

## Routing
Update [frontend/src/App.tsx](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/src/App.tsx) to add an admin-only route.

Suggested route:
- `/app/admin/loan-imports`

Protect it using the same role-based approach already used for admin pages.

## New Page
Create:
- `frontend/src/pages/AdminLoanImportsPage.tsx`

Responsibilities:
- upload file
- call preview endpoint
- render preview table
- execute import
- show results
- show recent import history

## API Client Additions
Update [frontend/src/api/client.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/src/api/client.ts) with functions such as:
- `previewLoanImport(file: File)`
- `runLoanImport(batchId: number)`
- `getLoanImports()`
- `getLoanImport(id: number)`
- `getLoanImportRows(id: number)`

Use multipart upload for preview.
Use normal JSON fetches for history/detail endpoints.

## Types
Add new frontend types in:
- `frontend/src/types/api.ts`

Needed shapes include:
- `LoanImportBatch`
- `LoanImportRow`
- `LoanImportPreviewResponse`
- `LoanImportExecuteResponse`

## Navigation
Decide where admins discover the feature.

Recommended options:
- add to admin navigation
- or add a link from Settings for admins only

Recommended first choice:
- add to admin navigation with clear label `Loan Imports`

## Loan UI Enhancements
To make imported ownership visible, extend the loan response and display metadata in loan views.

Update:
- [LoanResponse.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/dto/response/LoanResponse.java)
- [ResponseMapper.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/mapper/ResponseMapper.java)
- frontend loan types/pages

Suggested displayed fields:
- external loan ID
- source system
- last synced at
- externally managed badge

This helps users understand which data is imported and should not be edited manually.

## CSV Format

Recommended first version sample:

```csv
sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt
CORE_BANKING,LN-100245,Acme Manufacturing LLC,5000000.00,2024-01-15,ACTIVE,2026-03-08T10:15:00Z
CORE_BANKING,LN-100246,Northbridge Logistics Group,8500000.00,2023-11-01,ACTIVE,2026-03-08T10:15:00Z
```

## Error Handling

### Preview Errors
Show row-level errors before import, such as:
- missing external loan ID
- invalid date
- negative principal amount
- unsupported status
- duplicate row in file

### Import-Time Errors
Possible runtime failures:
- DB constraint conflicts
- stale preview batch state
- unexpected enum mapping issues

The results screen should distinguish:
- validation errors
- import execution failures
- rows skipped because unchanged

## Testing Plan

## Backend Unit Tests
Add tests for:
- create new external loan
- update existing external loan
- unchanged import row
- invalid status transition
- duplicate row detection in file
- restricted-field preservation
- status close behavior

Likely files:
- `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java`
- `src/test/java/com/covenantiq/service/LoanImportServiceTest.java`

## Backend Integration Tests
Add integration tests for:
- preview endpoint success
- preview endpoint invalid CSV
- import execution success
- import execution with mixed valid/invalid rows
- admin authorization required
- import history retrieval

Likely file:
- `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java`

## Frontend Tests
Add Playwright or component coverage for:
- admin can open Loan Imports page
- file preview renders results
- import execution shows summary
- invalid file surfaces validation errors
- non-admin cannot access page

## Data Migration / Compatibility

Because the app currently uses H2 with Hibernate update mode, schema evolution is simple in local/demo environments.

Still, code should assume:
- existing loans may have null `externalLoanId`
- existing loans may have null `sourceSystem`
- existing manual loans remain fully supported

Manual loans and imported loans should coexist.

## Rollout Plan

### Phase 1
- add loan sync metadata to `Loan`
- add repositories and import entities
- implement CSV preview and execute endpoints
- add admin UI page
- add import history view

### Phase 2
- add loan metadata display in loan pages
- add downloadable example CSV
- improve unchanged-row detection and row-level diff display

### Phase 3
- optionally add machine-to-machine sync endpoint under `/api/v1/external/**`
- optionally add scheduled automation outside this feature

## Recommended File-Level Changes

### Backend Changes
Modify:
- [src/main/java/com/covenantiq/domain/Loan.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/domain/Loan.java)
- [src/main/java/com/covenantiq/repository/LoanRepository.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/repository/LoanRepository.java)
- [src/main/java/com/covenantiq/config/SecurityConfig.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/config/SecurityConfig.java)
- [src/main/java/com/covenantiq/dto/response/LoanResponse.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/dto/response/LoanResponse.java)
- [src/main/java/com/covenantiq/mapper/ResponseMapper.java](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/src/main/java/com/covenantiq/mapper/ResponseMapper.java)

Add:
- `src/main/java/com/covenantiq/controller/AdminLoanImportController.java`
- `src/main/java/com/covenantiq/service/ExternalLoanSyncService.java`
- `src/main/java/com/covenantiq/service/LoanImportService.java`
- `src/main/java/com/covenantiq/service/CsvLoanImportParser.java`
- `src/main/java/com/covenantiq/domain/LoanImportBatch.java`
- `src/main/java/com/covenantiq/domain/LoanImportRow.java`
- `src/main/java/com/covenantiq/repository/LoanImportBatchRepository.java`
- `src/main/java/com/covenantiq/repository/LoanImportRowRepository.java`
- request/response DTOs for preview, execute, batch detail, and row detail

### Frontend Changes
Modify:
- [frontend/src/App.tsx](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/src/App.tsx)
- [frontend/src/api/client.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/src/api/client.ts)
- [frontend/src/types/api.ts](/c:/Users/6alal/OneDrive/Desktop/CovenantIQ/frontend/src/types/api.ts)
- admin navigation components if applicable

Add:
- `frontend/src/pages/AdminLoanImportsPage.tsx`
- optional supporting components for preview table and history table

## Open Product Decisions

These need explicit answers before implementation is finalized:

1. Should imports be allowed to reopen closed loans?
Recommended: no, not in v1.

2. Should a preview batch expire after a short time?
Recommended: yes, optional 30-60 minute validity.

3. Should import execute valid rows if some rows are invalid?
Recommended: yes, but only after preview clearly labels invalid rows.

4. Should admins be allowed to edit imported loan master fields manually later?
Recommended: no, or at minimum mark such edits as overrides.

5. Should files be stored after import?
Recommended: store metadata and row outcomes first; storing the original file is optional.

## Recommended First Release Decision Set

To keep the first implementation tight:
- CSV only
- admin-only page
- preview before execute
- source-managed loan fields only
- no reopen of closed loans
- no direct machine-to-machine endpoint yet
- no live push; rely on normal page reload/refetch
- import history retained in DB

## Summary

The recommended implementation is an admin-owned CSV import feature that creates or updates externally managed loan master records while preserving CovenantIQ as the owner of monitoring and analyst workflow data.

This design fits the current repo because:
- the loan is already the parent monitoring object
- downstream monitoring is already modeled separately
- security already supports admin-only screens
- the frontend already has an admin area pattern
- the backend already uses service/controller/repository layering that can absorb this cleanly
