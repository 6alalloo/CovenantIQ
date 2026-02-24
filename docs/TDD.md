# CovenantIQ Technical Design Document (TDD)

## 1. Technical Summary

CovenantIQ is a full-stack application composed of:
- **Backend:** Java 21, Spring Boot, Spring Web, Spring Data JPA, H2
- **Frontend:** React + TypeScript + Vite + Tailwind CSS + Recharts
- **Deployment:** Single Docker container where Spring Boot serves built SPA assets

Architecture emphasizes deterministic domain logic, clean layering, and auditable financial risk outputs.

## 2. System Architecture

### Backend Layering
- `controller`: REST endpoints only
- `service`: business logic
- `repository`: JPA persistence
- `domain`: JPA entities
- `dto`: request/response contracts
- `enums`: constrained business values
- `exception`: custom exceptions and problem-detail mapping
- `config`: OpenAPI, CORS, app configuration

### Frontend Structure
- `src/app`: app bootstrap and routing
- `src/features/auth`: mock login/session
- `src/features/loans`: list/detail/create loan views
- `src/features/covenants`: covenant creation forms
- `src/features/statements`: statement submission forms
- `src/features/risk`: alerts, results, risk summary, charts
- `src/shared`: API client, UI components, constants, utilities

## 3. Domain Model

### Entities
- `Loan`
  - id, borrowerName, principalAmount, startDate, status
- `Covenant`
  - id, loan, type, thresholdValue, comparisonType, severityLevel
- `FinancialStatement`
  - id, loan, periodType (`QUARTERLY`, `ANNUAL`), fiscalYear, fiscalQuarter (nullable for annual)
  - currentAssets, currentLiabilities, totalDebt, totalEquity, ebit, interestExpense
  - submissionTimestampUtc, superseded
- `CovenantResult`
  - id, covenant, financialStatement, actualValue, status, evaluationTimestampUtc, superseded
- `Alert`
  - id, loan, alertType, message, severityLevel, triggeredTimestampUtc
  - financialStatement, alertRuleCode, superseded

### Constraints
- Unique: `(loan_id, covenant_type)`
- Unique active statement period: `(loan_id, period_type, fiscal_year, fiscal_quarter, superseded=false)`

## 4. Enums

- `LoanStatus`: `ACTIVE`, `CLOSED`
- `CovenantType`: `CURRENT_RATIO`, `DEBT_TO_EQUITY`
- `ComparisonType`: `GREATER_THAN_EQUAL`, `LESS_THAN_EQUAL`
- `SeverityLevel`: `LOW`, `MEDIUM`, `HIGH`
- `CovenantResultStatus`: `PASS`, `BREACH`
- `AlertType`: `BREACH`, `EARLY_WARNING`
- `PeriodType`: `QUARTERLY`, `ANNUAL`
- `RiskLevel`: `LOW`, `MEDIUM`, `HIGH`

## 5. Service Design

### FinancialRatioService
- `calculateCurrentRatio(statement): BigDecimal`
- `calculateDebtToEquity(statement): BigDecimal`
- Uses fixed scale and `RoundingMode.HALF_UP`
- Throws validation exception (422) for zero denominators

### CovenantEvaluationService
- Load active covenants for loan
- Compute ratio by covenant type
- Compare with threshold by operator
- Save result
- Generate breach alerts for failed conditions

### TrendAnalysisService
- Evaluate Rule 1 and Rule 2 after statement persistence
- Rule 1: detect 3 consecutive current-ratio declines by cadence stream
- Rule 2: directional near-threshold (within 5% toward breach direction)
- Generate EARLY_WARNING alerts per triggered rule

### RiskSummaryService
- Aggregate active, non-superseded artifacts
- Active warnings = warnings from latest evaluation cycle
- Apply risk ladder

### LoanLifecycleService
- Close loan endpoint support
- Enforce state rules (no new covenants/statements when closed)

## 6. API Design

Base path: `/api/v1`

- `POST /loans`
- `GET /loans` (paginated)
- `GET /loans/{id}`
- `PATCH /loans/{id}/close`
- `POST /loans/{loanId}/covenants`
- `POST /loans/{loanId}/financial-statements`
- `GET /loans/{loanId}/covenant-results` (paginated)
- `GET /loans/{loanId}/alerts` (paginated)
- `GET /loans/{loanId}/risk-summary`

### Error Contract
- RFC7807 via `application/problem+json`
- Status semantics:
  - 404: resource not found
  - 409: lifecycle/business-state conflict
  - 422: validation/business rule failure

## 7. Transaction and Consistency

- Statement submission flow is atomic:
  1. Upsert statement and supersede prior active version
  2. Evaluate covenants
  3. Run trend analysis
  4. Persist results/alerts
- If any step fails, transaction rolls back.

## 8. Frontend Integration Design

- Frontend uses typed API client and DTO-aligned models.
- Mock auth is client-side only:
  - fixed demo users
  - route guards in SPA
  - backend remains unauthenticated per requirement
- Views:
  - Loans table and detail page
  - Covenant creation modal/page
  - Statement submission form with cadence-aware period input
  - Alerts/results tables with sorting and pagination controls
  - Risk summary panel + Recharts trend visualization

## 9. Build and Deployment

- Maven build produces backend jar.
- Frontend build artifacts are copied into Spring static resources during build stage.
- Single runtime container serves:
  - API endpoints
  - static SPA assets (fallback to index route for client-side routing)

## 10. Testing Strategy

### Unit Tests
- Ratio calculations and rounding behavior
- Comparison logic for both operators
- Near-threshold directional boundaries
- Consecutive decline rule detection
- Risk ladder outcomes

### Integration Tests
- End-to-end backend flow from loan creation through risk summary
- Upsert + supersession behavior
- Closed-loan conflict behavior
- RFC7807 error payload assertions
- Pagination and sorting behavior

### Frontend Tests
- Core component/render tests for key screens
- API client contract tests (mocked)
- Smoke user flow test (mock login -> submit statement -> view risk output)

