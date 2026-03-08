# Loan Import Execution Checklist

## Phase 0: Finalize Product Decisions
- [ ] Confirm imported loans are matched only by `sourceSystem + externalLoanId`
- [ ] Confirm imported loans cannot auto-reopen from `CLOSED` to `ACTIVE` in v1
- [ ] Confirm CSV is the only supported import format in v1
- [ ] Confirm import execution may proceed for valid rows even if some rows are invalid
- [ ] Confirm original uploaded file storage is not required in v1

## Phase 1: Backend Data Model
- [ ] Add external sync fields to `Loan`
- [ ] Add unique constraint for `sourceSystem + externalLoanId`
- [ ] Add `LoanImportBatch` entity
- [ ] Add `LoanImportRow` entity
- [ ] Add batch/row status enums if needed
- [ ] Extend `LoanRepository` with external lookup query
- [ ] Add repositories for import batches and rows

## Phase 1: Backend DTOs
- [ ] Add CSV row DTO for parsed import data
- [ ] Add preview response DTOs
- [ ] Add execute response DTOs
- [ ] Add import history/detail response DTOs

## Phase 1: Backend Services
- [ ] Add CSV parser service for loan imports
- [ ] Add row validation logic
- [ ] Add `ExternalLoanSyncService` for create/update behavior
- [ ] Add `LoanImportService` for preview and execute orchestration
- [ ] Add activity logging for imported loan changes
- [ ] Add optional outbox events for imported loan create/update

## Phase 1: Backend API And Security
- [ ] Add admin-only controller for preview/execute/history endpoints
- [ ] Add multipart preview endpoint
- [ ] Add execute-by-batch endpoint
- [ ] Add batch list endpoint
- [ ] Add batch detail/row results endpoint
- [ ] Restrict endpoints to `ADMIN`

## Phase 1: Frontend Types And API Client
- [ ] Add loan import types to `frontend/src/types/api.ts`
- [ ] Add preview/import/history functions to `frontend/src/api/client.ts`
- [ ] Extend `Loan` type with external sync metadata

## Phase 1: Frontend UI
- [ ] Add admin-only route for Loan Imports
- [ ] Add nav entry for Loan Imports
- [ ] Build `AdminLoanImportsPage`
- [ ] Implement CSV upload and preview flow
- [ ] Implement import execution action
- [ ] Implement import history list and batch detail rendering
- [ ] Show row-level validation/action results

## Phase 1: Loan Visibility Enhancements
- [ ] Extend backend `LoanResponse`
- [ ] Extend response mapping
- [ ] Surface external metadata in loan-facing UI where useful

## Phase 1: Testing
- [ ] Add backend unit tests for sync logic
- [ ] Add backend unit tests for import parsing/validation
- [ ] Add integration test for preview endpoint
- [ ] Add integration test for execute endpoint
- [ ] Add frontend coverage for admin route access and import flow

## Phase 2: UX Hardening
- [ ] Add downloadable sample CSV
- [ ] Add clearer diff/details for `UPDATE` rows
- [ ] Add preview expiry handling
- [ ] Add better empty/error states in import history

## Phase 3: Optional Expansion
- [ ] Add machine-to-machine `/api/v1/external/**` path if needed later
- [ ] Add scheduled automated sync outside admin UI flow
- [ ] Add import notifications if operators need them
