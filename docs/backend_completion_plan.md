# CovenantIQ Backend Completion Plan

## Executive Summary

This document provides a comprehensive implementation plan to complete the CovenantIQ backend based on gap analysis between the requirements.md spec and current implementation state.

**Requirements Breakdown:**
- **Total Requirements:** 45 (23 Phase 1 + 22 Phase 2)
- **Validated and Working:** 23 requirements (51%) - Phase 1 complete
- **Code Added but Not Validated:** 7 requirements (16%) - Phase 2A backend exists but untested
- **Completely Missing:** 15 requirements (33%) - Phase 2B not implemented

**Current State:** 
- Phase 1: 100% complete and validated (23/23 requirements)
- Phase 2A: Backend code added but NOT validated (7/22 Phase 2 requirements = 32% of Phase 2)
- Phase 2B: No implementation (15/22 Phase 2 requirements = 68% of Phase 2)

**Target State:** Full Phase 2 implementation with all 22 Phase 2 requirements validated and working.

**Estimated Completion:** 8-10 implementation phases covering the 49% of total requirements that are either unvalidated or missing.

---

## Gap Analysis Summary

### Phase 1: 23/23 Requirements Complete ✓ (100%)

**Implemented and Validated:**
- P1-1 to P1-5: Loan CRUD, covenant creation, financial statement submission
- P1-6 to P1-7: Current Ratio and Debt-to-Equity calculations (2 covenant types only)
- P1-8 to P1-9: Covenant evaluation and breach alert generation
- P1-10 to P1-11: Early warnings (consecutive decline, near-threshold)
- P1-12 to P1-14: Covenant results retrieval, alert retrieval, risk summary
- P1-15: RFC7807 error handling
- P1-16 to P1-20: Frontend UI (mock auth, loan management, covenant management, statement submission, risk visualization)
- P1-21: OpenAPI documentation
- P1-22: Demo data seeding
- P1-23: Single container deployment

### Phase 2A: 7/22 Requirements - Backend Code Added, Not Validated ⚠️ (32% of Phase 2)

**Code Exists but Needs Validation:**
- Req 1: Risk details endpoint (GET /api/v1/loans/{id}/risk-details)
- Req 2-3: Alert lifecycle management (OPEN → ACKNOWLEDGED → UNDER_REVIEW → RESOLVED)
- Req 4-5: CSV exports (alerts, covenant results)
- Req 10-12: JWT authentication, password security, RBAC (backend only, frontend not integrated)
- Req 17: Structured logging with correlation IDs
- Req 18: Health endpoint hardening

**Critical Gap:** All Phase 2A code lacks unit tests, integration tests, and end-to-end validation.

### Phase 2B: 15/22 Requirements - Not Implemented ✗ (68% of Phase 2)
**Completely Missing:**
- Req 6: Additional covenant types (6 new types: DSCR, INTEREST_COVERAGE, TANGIBLE_NET_WORTH, DEBT_TO_EBITDA, FIXED_CHARGE_COVERAGE, QUICK_RATIO)
- Req 7: Financial statement schema extensions (8 new fields: netOperatingIncome, totalDebtService, intangibleAssets, ebitda, fixedCharges, inventory, totalAssets, totalLiabilities)
- Req 8: Bulk import (CSV/Excel parsing with row-level validation)
- Req 9: Portfolio-wide risk aggregation (endpoint exists but may need validation)
- Req 13: User management endpoints (CRUD for users and roles)
- Req 14: Document attachments (PDF blob storage)
- Req 15: Loan comments and collaboration
- Req 16: Activity logging and audit trail
- Req 19: Volatility detection (std dev > 0.3)
- Req 20: Seasonal anomaly detection (>25% deviation from same quarter)
- Req 21: CSV parser for bulk import
- Req 22: Excel parser for bulk import

### Summary Statistics

| Category | Requirements | Percentage of Total | Percentage of Phase 2 |
|----------|--------------|---------------------|----------------------|
| **Phase 1 Complete** | 23/45 | 51% | N/A |
| **Phase 2A (Code Added, Not Validated)** | 7/45 | 16% | 32% of Phase 2 |
| **Phase 2B (Not Implemented)** | 15/45 | 33% | 68% of Phase 2 |
| **Total Remaining Work** | 22/45 | 49% | 100% of Phase 2 |

**Key Insight:** While Phase 1 is solid, Phase 2 is only 32% coded (and 0% validated). The majority of Phase 2 work (68%) has no implementation at all.

---

## Implementation Phases

### Phase 0: Validation and Stabilization (PRIORITY 1)

**Objective:** Validate and stabilize Phase 2A backend code that was added but never tested. This covers 7 requirements (16% of total, 32% of Phase 2).

**Scope:**
- Req 1: Risk details endpoint
- Req 2-3: Alert lifecycle management
- Req 4-5: CSV exports
- Req 10-12: JWT auth, password security, RBAC
- Req 17: Structured logging
- Req 18: Health endpoint

**Tasks:**
1. [x] Write unit tests for AuthService (login, refresh, password hashing, lockout) - Completed 2026-02-25
2. [x] Write unit tests for AlertService (lifecycle transitions, resolution validation) - Completed 2026-02-25
3. [x] Write unit tests for RiskSummaryService (risk details endpoint) - Completed 2026-02-25
4. [x] Write unit tests for PortfolioSummaryService (aggregation logic) - Completed 2026-02-25
5. [x] Write unit tests for ExportService (CSV generation, escaping) - Completed 2026-02-25
6. [x] Write integration tests for auth flow (login -> protected endpoint -> refresh) - Completed 2026-02-25
7. [x] Write integration tests for alert lifecycle (create -> acknowledge -> review -> resolve) - Completed 2026-02-25
8. [x] Write integration tests for RBAC enforcement (403 for forbidden actions) - Completed 2026-02-25
9. [x] Validate correlation ID propagation in logs and response headers - Completed 2026-02-25
10. [x] Validate health endpoint is unauthenticated and returns component status - Completed 2026-02-25
11. [x] Run full test suite and fix any failures - Completed 2026-02-25
12. Docker smoke test: build, run, verify all Phase 2A endpoints

**Acceptance Criteria:**
- All Phase 2A unit tests pass with >80% coverage (7 requirements validated)
- All Phase 2A integration tests pass
- Docker container builds and runs successfully
- Health endpoint returns 200 without auth
- JWT auth flow works end-to-end
- Alert lifecycle enforces valid transitions
- RBAC returns 403 for unauthorized actions
- Logs include correlationId
- CSV exports download with correct format

**Estimated Effort:** 3-4 days

**Impact:** Validates 16% of total requirements (32% of Phase 2)

---

### Phase 1: Financial Statement Schema Extension

**Objective:** Extend FinancialStatement entity to support additional covenant types.


**Current State:** FinancialStatement has 6 fields (currentAssets, currentLiabilities, totalDebt, totalEquity, ebit, interestExpense).

**Target State:** Add 8 new fields to support 6 additional covenant types.

**Tasks:**
1. [x] Add fields to FinancialStatement entity - Completed 2026-02-26:
   - netOperatingIncome (BigDecimal, precision 19, scale 4)
   - totalDebtService (BigDecimal, precision 19, scale 4)
   - intangibleAssets (BigDecimal, precision 19, scale 4)
   - ebitda (BigDecimal, precision 19, scale 4)
   - fixedCharges (BigDecimal, precision 19, scale 4)
   - inventory (BigDecimal, precision 19, scale 4)
   - totalAssets (BigDecimal, precision 19, scale 4)
   - totalLiabilities (BigDecimal, precision 19, scale 4)
2. [x] Update SubmitFinancialStatementRequest DTO with new fields (all optional) - Completed 2026-02-26
3. [x] Update FinancialStatementResponse DTO with new fields - Completed 2026-02-26
4. [x] Update DataInitializer to populate new fields in demo data - Completed 2026-02-26
5. [x] Write unit tests for field validation (non-negative constraints) - Completed 2026-02-26
6. [x] Update OpenAPI documentation - Completed 2026-02-26 (via updated API DTO contracts)

**Acceptance Criteria:**
- FinancialStatement entity has all 14 fields
- Request/response DTOs include new fields
- Demo data includes realistic values for new fields
- Validation enforces non-negative constraints
- Tests pass for extended schema

**Estimated Effort:** 1 day

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

### Phase 2: Additional Covenant Types

**Objective:** Implement 6 new covenant types with ratio calculations. This covers Requirement 6 (2% of total, 5% of Phase 2).

**Current State:** Only CURRENT_RATIO and DEBT_TO_EQUITY supported.

**Target State:** Support 8 total covenant types.

**Tasks:**
1. [x] Update CovenantType enum with new values - Completed 2026-02-26:
   - DSCR (Debt Service Coverage Ratio)
   - INTEREST_COVERAGE
   - TANGIBLE_NET_WORTH
   - DEBT_TO_EBITDA
   - FIXED_CHARGE_COVERAGE
   - QUICK_RATIO
2. [x] Add calculation methods to FinancialRatioService - Completed 2026-02-26:
   - calculateDSCR: netOperatingIncome / totalDebtService
   - calculateInterestCoverage: ebit / interestExpense
   - calculateTangibleNetWorth: totalAssets - intangibleAssets - totalLiabilities
   - calculateDebtToEBITDA: totalDebt / ebitda
   - calculateFixedChargeCoverage: (ebit + fixedCharges) / (fixedCharges + interestExpense)
   - calculateQuickRatio: (currentAssets - inventory) / currentLiabilities
3. [x] Update CovenantEvaluationService to route to correct calculation method - Completed 2026-02-26
4. [x] Add validation for division by zero in each calculation - Completed 2026-02-26
5. [x] Write unit tests for each ratio calculation (happy path, edge cases, division by zero) - Completed 2026-02-26
6. [x] Write integration tests for covenant evaluation with new types - Completed 2026-02-26
7. [x] Update demo data to include covenants using new types - Completed 2026-02-26

**Acceptance Criteria:**
- All 8 covenant types are supported
- Each ratio calculation uses BigDecimal with HALF_UP rounding to 4 decimals
- Division by zero throws UnprocessableEntityException with HTTP 422
- Unit tests cover all calculation paths
- Integration tests verify end-to-end evaluation
- Demo data includes examples of each covenant type

**Estimated Effort:** 2 days

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

### Phase 3: Enhanced Early Warning Detection

**Objective:** Add volatility and seasonal anomaly detection rules. This covers Requirements 19-20 (4% of total, 9% of Phase 2).

**Current State:** Only consecutive decline and near-threshold warnings implemented.

**Target State:** 4 early warning rules active.

**Tasks:**
1. [x] Add volatility detection to TrendAnalysisService - Completed 2026-02-26:
   - Fetch last 4 statements for same period type
   - Calculate standard deviation of ratio values
   - If std dev > 0.3, create EARLY_WARNING alert with severity MEDIUM
   - Require minimum 4 historical periods
2. [x] Add seasonal anomaly detection to TrendAnalysisService - Completed 2026-02-26:
   - Compare current quarter ratio to same quarter previous year
   - Calculate percentage deviation: |((current - historical) / historical)| * 100
   - If deviation > 25%, create EARLY_WARNING alert with severity LOW
   - Require minimum 4 quarters of historical data
3. [x] Add helper methods for statistical calculations (standard deviation, percentage deviation) - Completed 2026-02-26
4. [x] Write unit tests for volatility detection (sufficient data, insufficient data, high volatility, low volatility) - Completed 2026-02-26
5. [x] Write unit tests for seasonal anomaly detection (sufficient data, insufficient data, anomaly detected, no anomaly) - Completed 2026-02-26
6. [x] Write integration tests for enhanced early warning flow - Completed 2026-02-26
7. [x] Update demo data to trigger volatility and seasonal anomaly alerts - Completed 2026-02-26


**Acceptance Criteria:**
- Volatility detection creates alerts when std dev > 0.3
- Seasonal anomaly detection creates alerts when deviation > 25%
- Both rules skip gracefully when insufficient historical data
- Unit tests cover all detection paths
- Integration tests verify alerts are created correctly
- Demo data includes examples of volatility and seasonal anomalies

**Estimated Effort:** 2 days

**Impact:** Implements 2 requirements (4% of total, 9% of Phase 2)

---

### Phase 4: Bulk Import (CSV/Excel)

**Objective:** Enable bulk financial statement upload via CSV and Excel files. This covers Requirements 8, 21-22 (7% of total, 14% of Phase 2).

**Current State:** Only single statement submission supported.

**Target State:** Bulk import with row-level validation and error reporting.

**Tasks:**
1. [x] Add dependencies to pom.xml - Completed 2026-02-26:
   - opencsv (CSV parsing)
   - apache-poi (Excel parsing)
2. [x] Create BulkImportService with methods - Completed 2026-02-26:
   - parseCSV(InputStream, Loan): List<FinancialStatementDTO>
   - parseExcel(InputStream, Loan): List<FinancialStatementDTO>
   - validateAndImport(List<FinancialStatementDTO>, Loan): BulkImportSummary
3. [x] Create DTOs - Completed 2026-02-26:
   - BulkImportSummary (totalRows, successCount, failureCount, rowResults)
   - RowResult (rowNumber, success, errorMessage)
4. [x] Add endpoint to LoanController - Completed 2026-02-26:
   - POST /api/v1/loans/{loanId}/financial-statements/bulk-import
   - Accept multipart/form-data with file parameter
   - Validate file size < 5MB (return HTTP 413 if exceeded)
   - Validate file type (CSV or XLSX)
5. [x] Implement row-level validation - Completed 2026-02-26:
   - Parse dates in ISO 8601 format (YYYY-MM-DD)
   - Infer periodType and fiscalQuarter from periodEndDate
   - Validate all monetary fields are non-negative
   - Check for duplicate periodEndDate (mark as failed)
   - Continue processing after row failures
6. [x] Write unit tests for CSV/Excel parsing and validation logic - Completed 2026-02-26
7. [x] Write integration tests for bulk import endpoint (success, partial failure, file too large) - Completed 2026-02-26
8. [x] Update OpenAPI documentation - Completed 2026-02-26 (controller + DTO contracts)

**Acceptance Criteria:**
- CSV and Excel files are parsed correctly
- Row-level validation reports specific errors
- Import continues after row failures
- Files > 5MB return HTTP 413
- Duplicate periods are detected and marked as failed
- Unit tests cover parsing and validation paths
- Integration tests verify end-to-end bulk import
- OpenAPI docs include bulk import endpoint

**Estimated Effort:** 3-4 days

**Impact:** Implements 3 requirements (7% of total, 14% of Phase 2)

---

### Phase 5: Document Attachments

**Objective:** Enable PDF attachment storage for financial statements. This covers Requirement 14 (2% of total, 5% of Phase 2).

**Current State:** No attachment support.

**Target State:** PDF attachments stored as database blobs with metadata.

**Tasks:**
1. [x] Create Attachment entity - Completed 2026-02-26:
   - id (Long, primary key)
   - financialStatement (ManyToOne, required)
   - filename (String, required)
   - fileSize (Long, required)
   - contentType (String, required, default "application/pdf")
   - fileData (byte[], Lob, required)
   - uploadedBy (ManyToOne User, required)
   - uploadedAt (OffsetDateTime, required)
2. [x] Create AttachmentRepository extending JpaRepository - Completed 2026-02-26
3. [x] Create AttachmentService with methods - Completed 2026-02-26:
   - uploadAttachment(Long statementId, MultipartFile file, User user): Attachment
   - getAttachmentMetadata(Long statementId): List<AttachmentMetadata>
   - downloadAttachment(Long attachmentId): AttachmentDownload
   - deleteAttachment(Long attachmentId, User user): void
4. [x] Create DTOs - Completed 2026-02-26:
   - AttachmentMetadata (id, filename, fileSize, contentType, uploadedBy, uploadedAt)
   - AttachmentDownload (filename, contentType, fileData)
5. [x] Add endpoints to new AttachmentController - Completed 2026-02-26:
   - POST /api/v1/financial-statements/{id}/attachments (multipart/form-data)
   - GET /api/v1/financial-statements/{id}/attachments (list metadata)
   - GET /api/v1/attachments/{id} (download file)
   - DELETE /api/v1/attachments/{id}
6. [x] Implement validation - Completed 2026-02-26:
   - Validate file type is PDF (return HTTP 415 for non-PDF)
   - Validate file size < 10MB (return HTTP 413 if exceeded)
   - Validate financial statement exists
7. [x] Implement authorization - Completed 2026-02-26:
   - Only ANALYST and ADMIN can upload/delete
   - All authenticated users can view/download
8. [x] Add cascade delete: when FinancialStatement deleted, delete attachments - Completed 2026-02-26
9. [x] Write unit/integration tests for attachment endpoints (upload, list, download, delete) - Completed 2026-02-26
10. [x] Update OpenAPI documentation - Completed 2026-02-26 (controller + DTO contracts)

**Acceptance Criteria:**
- PDF files are stored as database blobs
- Metadata is tracked (filename, size, uploader, timestamp)
- Files > 10MB return HTTP 413
- Non-PDF files return HTTP 415
- Cascade delete works when statement deleted
- Unit tests cover service logic
- Integration tests verify end-to-end attachment flow
- OpenAPI docs include attachment endpoints

**Estimated Effort:** 2-3 days

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

### Phase 6: Loan Comments and Collaboration

**Objective:** Enable team collaboration through loan comments. This covers Requirement 15 (2% of total, 5% of Phase 2).

**Current State:** No comment support.

**Target State:** Users can add, view, and delete comments on loans.

**Tasks:**
1. [x] Create Comment entity - Completed 2026-02-26:
   - id (Long, primary key)
   - loan (ManyToOne, required)
   - createdBy (ManyToOne User, required)
   - commentText (String, length 5000, required)
   - createdAt (OffsetDateTime, required)
2. [x] Create CommentRepository extending JpaRepository - Completed 2026-02-26
3. [x] Create CommentService with methods - Completed 2026-02-26:
   - addComment(Long loanId, String commentText, User user): Comment
   - getComments(Long loanId, Pageable pageable): Page<Comment>
   - deleteComment(Long commentId, User user): void
4. [x] Create DTOs - Completed 2026-02-26:
   - CreateCommentRequest (commentText)
   - CommentResponse (id, commentText, createdBy username, createdAt)
5. [x] Add endpoints to LoanController - Completed 2026-02-26:
   - POST /api/v1/loans/{loanId}/comments
   - GET /api/v1/loans/{loanId}/comments (paginated, sorted by createdAt desc)
   - DELETE /api/v1/loans/{loanId}/comments/{commentId}
6. [x] Implement validation - Completed 2026-02-26:
   - Validate commentText length <= 5000 characters
   - Validate loan exists
7. [x] Implement authorization - Completed 2026-02-26:
   - All authenticated users can add comments
   - Only comment creator or ADMIN can delete
8. [x] Write unit/integration tests for comment endpoints (create, list, delete, forbidden delete) - Completed 2026-02-26
9. [x] Update OpenAPI documentation - Completed 2026-02-26 (controller + DTO contracts)

**Acceptance Criteria:**
- Users can add comments to loans
- Comments are paginated and sorted by createdAt descending
- Only creator or ADMIN can delete comments
- Comment length is limited to 5000 characters
- Unit tests cover service logic
- Integration tests verify end-to-end comment flow
- OpenAPI docs include comment endpoints

**Estimated Effort:** 1-2 days

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

### Phase 7: Activity Logging and Audit Trail

**Objective:** Maintain comprehensive audit trail of user actions. This covers Requirement 16 (2% of total, 5% of Phase 2).

**Current State:** No activity logging.

**Target State:** All significant business events are logged with metadata.


**Tasks:**
1. [x] Create ActivityEventType enum - Completed 2026-02-26:
   - LOAN_CREATED, LOAN_UPDATED, LOAN_CLOSED
   - COVENANT_CREATED, COVENANT_UPDATED
   - STATEMENT_SUBMITTED
   - ALERT_ACKNOWLEDGED, ALERT_RESOLVED
   - COMMENT_ADDED, COMMENT_DELETED
   - USER_CREATED, USER_UPDATED, USER_DEACTIVATED
2. [x] Create ActivityLog entity - Completed 2026-02-26:
   - id (Long, primary key)
   - eventType (ActivityEventType, required)
   - entityType (String, required, e.g., "Loan", "Alert")
   - entityId (Long, required)
   - user (ManyToOne User, nullable for system events)
   - username (String, denormalized for deleted users)
   - timestamp (OffsetDateTime, required)
   - description (String, length 1000, required)
3. [x] Create ActivityLogRepository extending JpaRepository - Completed 2026-02-26
4. [x] Create ActivityLogger service with methods - Completed 2026-02-26:
   - logEvent(ActivityEventType, String entityType, Long entityId, User user, String description)
   - getActivityForLoan(Long loanId, Pageable pageable): Page<ActivityLog>
   - getActivityForDateRange(LocalDate start, LocalDate end, Pageable pageable): Page<ActivityLog>
5. [x] Create DTOs - Completed 2026-02-26:
   - ActivityLogResponse (id, eventType, entityType, entityId, username, timestamp, description)
6. [x] Add endpoints to new ActivityController - Completed 2026-02-26:
   - GET /api/v1/loans/{loanId}/activity (paginated, sorted by timestamp desc)
   - GET /api/v1/activity (paginated, filtered by date range)
7. [x] Integrate logging into existing services - Completed 2026-02-26:
   - LoanService: log LOAN_CREATED, LOAN_UPDATED, LOAN_CLOSED
   - CovenantService: log COVENANT_CREATED
   - FinancialStatementService: log STATEMENT_SUBMITTED
   - AlertService: log ALERT_ACKNOWLEDGED, ALERT_RESOLVED
   - CommentService: log COMMENT_ADDED, COMMENT_DELETED
8. [x] Implement retention policy - Completed 2026-02-26:
   - Add scheduled job to delete logs older than 90 days
   - Use @Scheduled annotation with cron expression
9. [x] Write integration tests for activity endpoints (get loan activity, get date range activity) - Completed 2026-02-26
10. [x] Update OpenAPI documentation - Completed 2026-02-26 (controller + DTO contracts)

**Acceptance Criteria:**
- All significant business events are logged
- Activity logs include human-readable descriptions
- Logs are paginated and sorted by timestamp descending
- Retention policy deletes logs older than 90 days
- Unit tests cover logging logic
- Integration tests verify activity retrieval
- OpenAPI docs include activity endpoints

**Estimated Effort:** 2-3 days

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

### Phase 8: User Management

**Objective:** Enable administrators to manage user accounts and roles. This covers Requirement 13 (2% of total, 5% of Phase 2).

**Current State:** UserAccount entity exists for auth, but no management endpoints.

**Target State:** Full CRUD for user management with role assignment.

**Tasks:**
1. [x] Refactor user model with current rolesCsv approach (no separate role table) - Completed 2026-02-26:
   - Current: UserAccount with rolesCsv field
   - Target: Separate User, Role, UserRole entities (if not already done)
   - If already using separate entities, skip this step
2. [x] Create UserService with methods - Completed 2026-02-26:
   - createUser(CreateUserRequest, User admin): User
   - getUsers(Pageable pageable): Page<User>
   - getUserById(Long userId): User
   - updateUserRoles(Long userId, List<String> roles, User admin): User
   - deactivateUser(Long userId, User admin): void
3. [x] Create DTOs - Completed 2026-02-26:
   - CreateUserRequest (username, password, email, roles)
   - UpdateUserRolesRequest (roles)
   - UserResponse (id, username, email, active, roles, createdAt)
4. [x] Add endpoints to new UserController - Completed 2026-02-26:
   - POST /api/v1/users (ADMIN only)
   - GET /api/v1/users (ADMIN only, paginated)
   - GET /api/v1/users/{id} (ADMIN only)
   - PATCH /api/v1/users/{id}/roles (ADMIN only)
   - DELETE /api/v1/users/{id} (ADMIN only, soft delete)
5. [x] Implement validation - Completed 2026-02-26:
   - Validate username uniqueness (return HTTP 409 on duplicate)
   - Validate password policy (min 8 chars, uppercase, lowercase, digit, special char)
   - Validate at least one role is assigned
   - Prevent deletion of last ADMIN user (return HTTP 400)
6. [x] Implement authorization - Completed 2026-02-26:
   - All user management endpoints require ADMIN role
   - Return HTTP 403 for non-admin users
7. [x] Write integration tests for user endpoints (create, list, update roles, delete, forbidden) - Completed 2026-02-26
8. [x] Update OpenAPI documentation - Completed 2026-02-26 (controller + DTO contracts)

**Acceptance Criteria:**
- Admins can create, view, update, and deactivate users
- Username uniqueness is enforced
- Password policy is enforced
- Last ADMIN cannot be deleted
- Only ADMIN role can access user management endpoints
- Unit tests cover service logic
- Integration tests verify end-to-end user management
- OpenAPI docs include user management endpoints

**Estimated Effort:** 2-3 days

**Impact:** Implements 1 requirement (2% of total, 5% of Phase 2)

---

## Implementation Summary by Phase

| Phase | Requirements Covered | % of Total | % of Phase 2 | Effort (days) |
|-------|---------------------|------------|--------------|---------------|
| Phase 0: Validation | 7 (Req 1-5, 10-12, 17-18) | 16% | 32% | 3-4 |
| Phase 1: Schema Extension | 1 (Req 7) | 2% | 5% | 1 |
| Phase 2: New Covenant Types | 1 (Req 6) | 2% | 5% | 2 |
| Phase 3: Enhanced Warnings | 2 (Req 19-20) | 4% | 9% | 2 |
| Phase 4: Bulk Import | 3 (Req 8, 21-22) | 7% | 14% | 3-4 |
| Phase 5: Attachments | 1 (Req 14) | 2% | 5% | 2-3 |
| Phase 6: Comments | 1 (Req 15) | 2% | 5% | 1-2 |
| Phase 7: Activity Logging | 1 (Req 16) | 2% | 5% | 2-3 |
| Phase 8: User Management | 1 (Req 13) | 2% | 5% | 2-3 |
| **Total** | **18 requirements** | **39%** | **85%** | **18-26 days** |

**Note:** Phase 0 validates 7 requirements that have code but no tests (16% of total). Phases 1-8 implement 11 new requirements (24% of total). Together, this completes 39% of all requirements and 85% of Phase 2 requirements. Requirement 9 (Portfolio Aggregation) may already be partially implemented and needs assessment.

---

## Testing Strategy

### Unit Testing
- Target: >80% line coverage for business logic modules
- Focus areas:
  - Financial ratio calculations (all 8 types)
  - Covenant evaluation logic
  - Early warning detection rules (all 4 types)
  - Alert lifecycle transitions
  - RBAC enforcement
  - CSV/Excel parsing
  - Validation logic

### Integration Testing
- Test critical paths end-to-end:
  - Auth flow: login → protected endpoint → refresh
  - Statement submission → covenant evaluation → alert generation
  - Alert lifecycle: create → acknowledge → review → resolve
  - Bulk import: upload → validation → import summary
  - Attachment flow: upload → list → download → delete
  - Comment flow: create → list → delete
  - Activity logging: action → log created → retrieve logs
  - User management: create → update roles → deactivate
  - User management: create → update roles → deactivate

### Property-Based Testing (Optional, Phase 2B)
- Financial math properties:
  - Ratio calculations are deterministic
  - Division by zero always throws exception
  - Rounding is consistent
- Parser properties:
  - Parse → format → parse produces equivalent objects
  - Invalid input always produces descriptive error

### Performance Testing
- Risk summary: < 500ms
- Portfolio summary: < 2s for 1000 active loans
- Statement submission + evaluation: < 1s
- Health endpoint: < 1s

---

## Database Migration Strategy

### Current State
- H2 in-memory database
- No migration tooling
- Schema created from JPA entities on startup

### Target State (Production Readiness)
- External database (PostgreSQL or MySQL)
- Flyway or Liquibase for schema versioning
- Migration scripts for schema changes

### Migration Plan (Future Phase)
1. Add Flyway dependency to pom.xml
2. Create baseline migration script (V1__baseline.sql) from current schema
3. Create incremental migration scripts for each phase:
   - V2__add_financial_statement_fields.sql (Phase 1)
   - V3__add_attachment_entity.sql (Phase 5)
   - V4__add_comment_entity.sql (Phase 6)
   - V5__add_activity_log_entity.sql (Phase 7)
4. Configure Flyway in application.properties
5. Test migrations against PostgreSQL and MySQL
6. Update deployment documentation

**Note:** This is deferred to production deployment phase. Current H2 in-memory approach is acceptable for demo/development.

---

## Deployment Considerations

### Current Deployment
- Single Docker container
- Spring Boot serves API + static frontend
- H2 in-memory database
- No external dependencies

### Production Deployment Recommendations
1. External database (PostgreSQL recommended)
2. Environment-specific configuration (dev, staging, prod)
3. Secret management (JWT secret, DB credentials)
4. Log aggregation (ELK stack or CloudWatch)
5. Monitoring and alerting (Prometheus + Grafana)
6. Backup and disaster recovery
7. SSL/TLS termination (reverse proxy or load balancer)
8. Rate limiting and DDoS protection


---

## Risk Mitigation

### Technical Risks

**Risk 1: Phase 2A Code Not Validated**
- Impact: High (security vulnerabilities, broken features)
- Likelihood: High (code added but not tested)
- Mitigation: Phase 0 prioritizes validation and stabilization

**Risk 2: Database Performance at Scale**
- Impact: Medium (slow queries, timeouts)
- Likelihood: Medium (H2 in-memory not production-ready)
- Mitigation: Add database indexes, implement pagination, plan PostgreSQL migration

**Risk 3: File Upload Security**
- Impact: High (malicious file uploads, DoS)
- Likelihood: Medium (bulk import and attachments accept files)
- Mitigation: Strict file type validation, size limits, virus scanning (future)

**Risk 4: Alert Fatigue**
- Impact: Medium (users ignore alerts)
- Likelihood: Medium (4 early warning rules can generate noise)
- Mitigation: Severity calibration, filtering, rule tuning based on feedback

**Risk 5: Test Coverage Gaps**
- Impact: Medium (bugs in production)
- Likelihood: Medium (rapid development)
- Mitigation: Enforce >80% coverage, mandatory integration tests for critical paths

### Schedule Risks

**Risk 1: Scope Creep**
- Impact: High (delayed completion)
- Likelihood: Medium (new requirements emerge)
- Mitigation: Strict phase boundaries, defer non-critical features to Phase 3

**Risk 2: Integration Complexity**
- Impact: Medium (delays in testing)
- Likelihood: Medium (frontend integration required)
- Mitigation: Backend-first approach, mock frontend for testing

---

## Success Criteria

### Functional Completeness
- ✓ All 23 Phase 1 requirements validated (51% of total)
- ✓ All 7 Phase 2A requirements validated (16% of total, 32% of Phase 2)
- ✓ All 11 Phase 2B requirements implemented and validated (24% of total, 50% of Phase 2)
- ✓ Requirement 9 (Portfolio Aggregation) assessed and completed if needed (2% of total, 5% of Phase 2)
- ✓ Total: 41-42 of 45 requirements complete (91-93%)
- ✓ All 8 covenant types supported
- ✓ All 4 early warning rules active
- ✓ Bulk import supports CSV and Excel
- ✓ PDF attachments stored and retrievable
- ✓ Comments and activity logging functional
- ✓ User management endpoints operational

### Quality Metrics
- ✓ >80% unit test coverage for business logic
- ✓ All integration tests pass
- ✓ All performance targets met (risk summary <500ms, portfolio <2s)
- ✓ Zero critical security vulnerabilities
- ✓ RFC7807 error responses for all error cases
- ✓ OpenAPI documentation complete and accurate

### Operational Readiness
- ✓ Docker container builds and runs successfully
- ✓ Health endpoint returns component status
- ✓ Structured JSON logs with correlation IDs
- ✓ Demo data includes examples of all features
- ✓ Deployment runbook updated


---

## Implementation Timeline

### Phase 0: Validation and Stabilization
- Duration: 3-4 days
- Dependencies: None
- Deliverables: All Phase 2A features validated and tested

### Phase 1: Financial Statement Schema Extension
- Duration: 1 day
- Dependencies: Phase 0 complete
- Deliverables: Extended FinancialStatement entity with 8 new fields

### Phase 2: Additional Covenant Types
- Duration: 2 days
- Dependencies: Phase 1 complete
- Deliverables: 6 new covenant types with ratio calculations

### Phase 3: Enhanced Early Warning Detection
- Duration: 2 days
- Dependencies: Phase 2 complete
- Deliverables: Volatility and seasonal anomaly detection

### Phase 4: Bulk Import (CSV/Excel)
- Duration: 3-4 days
- Dependencies: Phase 1 complete
- Deliverables: Bulk import endpoint with row-level validation

### Phase 5: Document Attachments
- Duration: 2-3 days
- Dependencies: Phase 0 complete
- Deliverables: PDF attachment storage and retrieval

### Phase 6: Loan Comments and Collaboration
- Duration: 1-2 days
- Dependencies: Phase 0 complete
- Deliverables: Comment CRUD endpoints

### Phase 7: Activity Logging and Audit Trail
- Duration: 2-3 days
- Dependencies: Phase 0 complete
- Deliverables: Activity logging integrated across all services

### Phase 8: User Management
- Duration: 2-3 days
- Dependencies: Phase 0 complete
- Deliverables: User management CRUD endpoints

**Total Estimated Duration:** 18-26 days (3.5-5 weeks)

**Requirements Coverage:**
- Phase 0: Validates 7 requirements (16% of total, 32% of Phase 2)
- Phases 1-8: Implements 11 requirements (24% of total, 50% of Phase 2)
- Total: Completes 18 requirements (40% of total, 82% of Phase 2)
- Remaining: Req 9 (Portfolio Aggregation) needs assessment - may already be done

**Critical Path:** Phase 0 → Phase 1 → Phase 2 → Phase 3

**Parallel Tracks:**
- After Phase 0: Phases 4, 5, 6, 7, 8 can be developed in parallel
- Recommended: 2-3 developers working on different phases simultaneously

**Resource Optimization:**
- Single developer: 18-26 days sequential
- Two developers: 10-15 days with parallel work
- Three developers: 8-12 days with optimal parallelization

---

## Appendix A: API Endpoint Summary

### Implemented (Phase 1)
- POST /api/v1/loans
- GET /api/v1/loans
- GET /api/v1/loans/{id}
- PATCH /api/v1/loans/{id}/close
- POST /api/v1/loans/{loanId}/covenants
- POST /api/v1/loans/{loanId}/financial-statements
- GET /api/v1/loans/{loanId}/covenant-results
- GET /api/v1/loans/{loanId}/alerts
- GET /api/v1/loans/{loanId}/risk-summary

### Implemented but Not Validated (Phase 2A)
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- GET /api/v1/loans/{loanId}/risk-details
- PATCH /api/v1/alerts/{alertId}/status
- GET /api/v1/portfolio/summary
- GET /api/v1/loans/{loanId}/alerts/export
- GET /api/v1/loans/{loanId}/covenant-results/export
- GET /actuator/health


### To Be Implemented (Phase 2B)
- POST /api/v1/loans/{loanId}/financial-statements/bulk-import
- POST /api/v1/financial-statements/{id}/attachments
- GET /api/v1/financial-statements/{id}/attachments
- GET /api/v1/attachments/{id}
- DELETE /api/v1/attachments/{id}
- POST /api/v1/loans/{loanId}/comments
- GET /api/v1/loans/{loanId}/comments
- DELETE /api/v1/loans/{loanId}/comments/{commentId}
- GET /api/v1/loans/{loanId}/activity
- GET /api/v1/activity
- POST /api/v1/users
- GET /api/v1/users
- GET /api/v1/users/{id}
- PATCH /api/v1/users/{id}/roles
- DELETE /api/v1/users/{id}

---

## Appendix B: Entity Relationship Diagram

```
User (Phase 2)
├── id
├── username (unique)
├── passwordHash
├── email
├── active
├── createdAt
└── roles (OneToMany UserRole)

Role (Phase 2)
├── id
├── name (ANALYST, RISK_LEAD, ADMIN)
└── description

UserRole (Phase 2)
├── id
├── user (ManyToOne User)
├── role (ManyToOne Role)
└── assignedAt

Loan (Phase 1)
├── id
├── borrowerName
├── principalAmount
├── startDate
├── status (ACTIVE, CLOSED)
├── covenants (OneToMany Covenant)
├── financialStatements (OneToMany FinancialStatement)
├── alerts (OneToMany Alert)
└── comments (OneToMany Comment, Phase 2)

Covenant (Phase 1)
├── id
├── loan (ManyToOne Loan)
├── type (8 types in Phase 2)
├── thresholdValue
├── comparisonType
├── severityLevel
└── covenantResults (OneToMany CovenantResult)

FinancialStatement (Phase 1 + Phase 2 extensions)
├── id
├── loan (ManyToOne Loan)
├── periodType
├── fiscalYear
├── fiscalQuarter
├── currentAssets
├── currentLiabilities
├── totalDebt
├── totalEquity
├── ebit
├── interestExpense
├── netOperatingIncome (Phase 2)
├── totalDebtService (Phase 2)
├── intangibleAssets (Phase 2)
├── ebitda (Phase 2)
├── fixedCharges (Phase 2)
├── inventory (Phase 2)
├── totalAssets (Phase 2)
├── totalLiabilities (Phase 2)
├── submissionTimestampUtc
├── superseded
├── covenantResults (OneToMany CovenantResult)
├── alerts (OneToMany Alert)
└── attachments (OneToMany Attachment, Phase 2)

CovenantResult (Phase 1)
├── id
├── covenant (ManyToOne Covenant)
├── financialStatement (ManyToOne FinancialStatement)
├── actualValue
├── status (PASS, BREACH)
├── evaluationTimestampUtc
└── superseded

Alert (Phase 1 + Phase 2 extensions)
├── id
├── loan (ManyToOne Loan)
├── financialStatement (ManyToOne FinancialStatement)
├── alertType (BREACH, EARLY_WARNING)
├── message
├── severityLevel
├── triggeredTimestampUtc
├── alertRuleCode
├── superseded
├── status (OPEN, ACKNOWLEDGED, UNDER_REVIEW, RESOLVED, Phase 2)
├── acknowledgedBy (ManyToOne User, Phase 2)
├── acknowledgedAt (Phase 2)
├── resolvedBy (ManyToOne User, Phase 2)
├── resolvedAt (Phase 2)
└── resolutionNotes (Phase 2)

Attachment (Phase 2)
├── id
├── financialStatement (ManyToOne FinancialStatement)
├── filename
├── fileSize
├── contentType
├── fileData (Lob)
├── uploadedBy (ManyToOne User)
└── uploadedAt

Comment (Phase 2)
├── id
├── loan (ManyToOne Loan)
├── createdBy (ManyToOne User)
├── commentText
└── createdAt

ActivityLog (Phase 2)
├── id
├── eventType
├── entityType
├── entityId
├── user (ManyToOne User, nullable)
├── username (denormalized)
├── timestamp
└── description
```

---

## Appendix C: Configuration Properties

### Current Configuration (application.properties)
```properties
# Server
server.port=8080

# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:covenantiq
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# Security
app.security.enabled=true
app.jwt.secret=${JWT_SECRET:your-secret-key-change-in-production}
app.jwt.access-token-minutes=60
app.jwt.refresh-token-days=7

# Logging
logging.level.com.covenantiq=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

### Recommended Production Configuration
```properties
# Server
server.port=8080

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/covenantiq
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Security
app.security.enabled=true
app.jwt.secret=${JWT_SECRET}
app.jwt.access-token-minutes=60
app.jwt.refresh-token-days=7

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging (JSON format)
logging.level.com.covenantiq=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

---

## Conclusion

This backend completion plan provides a comprehensive roadmap to finish the CovenantIQ backend implementation based on accurate gap analysis.

**Accurate Assessment:**
- **Phase 1 (Complete):** 23/45 requirements (51%) - Fully validated and working
- **Phase 2A (Needs Validation):** 7/45 requirements (16%) - Code exists but untested
- **Phase 2B (Missing):** 15/45 requirements (33%) - No implementation
- **Total Remaining Work:** 22/45 requirements (49%)

**Corrected Understanding:**
The initial "~70% missing" assessment from the spec was more accurate than the "~30% missing" estimate. The backend is 51% complete (Phase 1 only), with 49% of requirements either unvalidated or completely missing.

**Key Takeaways:**
1. Phase 0 (validation) is critical - 16% of requirements have code but zero tests
2. Phase 2B represents 33% of total requirements with no code at all
3. Phases 1-3 form the critical path for covenant functionality (9% of total)
4. Phases 4-8 can be parallelized for faster delivery (15% of total)
5. Total estimated effort: 18-26 days (3.5-5 weeks) for single developer
6. With 2-3 developers: 8-15 days with parallel execution

**Success Metrics:**
- Completing all phases achieves 91-93% of total requirements (41-42 of 45)
- Remaining 3-4 requirements may already be partially implemented (Req 9) or deferred
- >80% test coverage ensures quality
- Performance targets validated (risk summary <500ms, portfolio <2s)

**Next Steps:**
1. Review and approve this plan with stakeholders
2. Allocate development resources (recommend 2-3 developers)
3. Begin Phase 0 validation immediately (highest priority)
4. Track progress against phase milestones and requirement completion
5. Adjust timeline based on actual velocity and discoveries

**Questions or Concerns:**
- Contact the engineering team for technical clarification
- Contact the product owner for scope prioritization
- Contact the project manager for timeline adjustments




