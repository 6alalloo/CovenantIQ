# Hardcoded / Mocked / Fake Data Audit

Date: 2026-03-06
Scope: `src`, `frontend`, `src/test`, `frontend/tests`, `src/main/resources`

## Summary

The codebase contains three distinct classes of non-real data:

- `prod-risk`: hardcoded values that affect runtime security or production behavior.
- `demo-only`: seeded, synthetic, or sample data that ships into runtime UX or bootstrap flows.
- `test-only`: mocks, fixtures, and fake payloads limited to automated tests.

## Prod-Risk

### 1. Hardcoded JWT secret in runtime config
- File: `src/main/resources/application.yml`
- Lines: 44-46

Finding:
- `app.jwt.secret` is hardcoded as `change-this-secret-for-prod-change-this-secret`.

Why it matters:
- This is a live runtime signing secret, not just sample content.
- If deployed unchanged, token forgery risk is straightforward.

### 2. Fallback webhook encryption key in runtime config
- File: `src/main/resources/application.yml`
- Line: 48

Finding:
- `app.webhook.encryption-key-base64` falls back to a fixed embedded key when the env var is absent.

Why it matters:
- This weakens secret separation across environments.
- A missing env var does not fail closed.

### 3. In-memory H2 database as default runtime datasource
- File: `src/main/resources/application.yml`
- Lines: 5-8

Finding:
- The application defaults to `jdbc:h2:mem:covenantiqdb`.

Why it matters:
- This is acceptable for local/demo use, but it is still a hardcoded runtime dependency profile.
- It implies ephemeral data and demo-oriented startup behavior unless overridden.

### 4. Bootstrapped alert workflow hardcoded in service code
- File: `src/main/java/com/covenantiq/service/WorkflowService.java`
- Lines: 68-102

Finding:
- The default alert workflow is created in code with fixed states, transitions, role gates, and required fields.
- Resolution transitions require hardcoded `resolutionNotes`.

Why it matters:
- This is runtime business behavior, not test scaffolding.
- It reduces configurability and can diverge from governed workflow definitions.

### 5. Hardcoded publish reason for bootstrapped workflow
- File: `src/main/java/com/covenantiq/service/WorkflowService.java`
- Line: 102

Finding:
- The workflow is published with the fixed reason `Bootstrapped default alert workflow`.

Why it matters:
- Lower severity than the items above, but still hardcoded operational metadata in audit history.

## Demo-Only

### 1. Seeded demo users with known passwords
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 88-97

Finding:
- Demo users are created with public-style usernames and the shared password `Demo123!`.
- Examples: `analyst@demo.com`, `risklead@demo.com`, `admin@demo.com`.

Impact:
- This is intentional seed/demo data, but it is runtime-accessible when seeding is enabled.

### 2. Seeded synthetic borrower portfolio
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 42-54

Finding:
- The app seeds 12 fixed borrower names, principal amounts, start dates, health labels, and one closed loan.

Examples:
- `Acme Manufacturing LLC`
- `Northbridge Logistics Group`
- `MetroBuild Contractors`

Impact:
- These are synthetic business records visible in runtime UI/API.

### 3. Hardcoded seeded covenant thresholds
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 134-176

Finding:
- Standard seeded covenants are assigned fixed thresholds and severities:
- `CURRENT_RATIO 1.20`
- `DEBT_TO_EQUITY 2.50`
- `DSCR 1.25`
- `INTEREST_COVERAGE 2.00`
- `DEBT_TO_EBITDA 4.00`
- `QUICK_RATIO 1.00`
- `FIXED_CHARGE_COVERAGE 1.35`

Impact:
- These are demo defaults driving seeded risk behavior.

### 4. Synthetic financial statement generation formulas
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 177-256

Finding:
- Quarterly statement history is algorithmically generated from health shifts, seasonal adjustments, clamps, and fixed date progression.

Impact:
- This is fake operational data, but it produces realistic-looking runtime trends, alerts, and covenant results.

### 5. Fabricated alert history and canned resolution text
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 270-319

Finding:
- Alerts are backfilled with hardcoded assignees, statuses, timestamps, and notes.
- Example notes:
  - `Resolved after borrower provided variance explanation and revised projections.`
  - `Under review with RM; pending covenant waiver decision.`

Impact:
- This makes the application look historically active even though the data is synthetic.

### 6. Seeded comment templates and authors
- File: `src/main/java/com/covenantiq/config/DataInitializer.java`
- Lines: 327-355

Finding:
- Comment text and authors are hardcoded and rotated across loans.
- Includes a canned closure comment for closed seeded loans.

Impact:
- Synthetic collaboration history appears in runtime views.

### 7. Login page prefilled with demo credentials
- File: `frontend/src/pages/LoginPage.tsx`
- Lines: 8-9, 39

Finding:
- Username defaults to `analyst@demo.com`.
- Password defaults to `Demo123!`.
- The page explicitly tells users to use seeded credentials.

Impact:
- Strong demo behavior in a runtime screen.
- Should not survive into a non-demo environment.

### 8. Portfolio page contains partly fake chart history
- File: `frontend/src/pages/PortfolioPage.tsx`
- Lines: 23-29

Finding:
- `trendData` contains hardcoded Sep-Jan values.
- Only the Feb point uses live summary values.

Impact:
- The chart is mixed real/fake data and can mislead users into believing the whole series is real.

### 9. Policy Studio ships with embedded sample policy data
- File: `frontend/src/pages/PolicyStudioPage.tsx`
- Lines: 17-33, 98-99, 148-150, 172-173, 207-224

Finding:
- Embedded sample rule JSON.
- Embedded sample validation input/output.
- Default change summary text.
- Autogenerated ruleset key/name defaults.
- Hardcoded publish reason.

Impact:
- This page is currently demo-driven rather than cleanly empty-state driven.

### 10. Loan overview covenant template catalog is hardcoded
- File: `frontend/src/pages/LoanOverviewPage.tsx`
- Lines: 22-105

Finding:
- Covenant template definitions, threshold defaults, guidance text, and category grouping are embedded in the component.

Impact:
- This is product behavior and UI scaffolding combined.
- Acceptable as seed UX, but still hardcoded business content.

### 11. Collateral / exception page is preloaded with fake business inputs
- File: `frontend/src/pages/LoanCollateralExceptionsPage.tsx`
- Lines: 21-24, 43-47, 55-60

Finding:
- Default form values:
  - `ACCOUNTS_RECEIVABLE`
  - `1000000`
  - `0.2`
  - `Temporary covenant waiver request`
- Submission also hardcodes `lienRank: 1` and `currency: "USD"`.

Impact:
- Runtime forms are demo-prefilled and partially fixed.

### 12. Integrations page ships with default webhook filters
- File: `frontend/src/pages/IntegrationsPage.tsx`
- Line: 18

Finding:
- The event filter field defaults to `AlertCreated,AlertStatusChanged`.

Impact:
- Low severity, but still hardcoded starter data in runtime UI.

### 13. Reports page starts with fixed historical dates
- File: `frontend/src/pages/ReportsPage.tsx`
- Lines: 27-28

Finding:
- Date range defaults to `2026-01-01` through `2026-02-26`.

Impact:
- Static dates will age and look incorrect.
- This is a hardcoded demo/reporting preset.

### 14. Change Control page can create a canned change request
- File: `frontend/src/pages/ChangeControlPage.tsx`
- Lines: 104-113, 144, 160

Finding:
- The page creates a synthetic ruleset change request with fixed justification, artifact metadata, and diff JSON.
- Release tags are autogenerated as `rel-{request.id}`.
- Rollback justification is canned.

Impact:
- Runtime governance UI contains explicit demo payload generation.

### 15. Admin user creation defaults to analyst role
- File: `frontend/src/pages/AdminUsersPage.tsx`
- Lines: 9, 14-19, 34

Finding:
- Role options are hardcoded.
- New-user form defaults to `ANALYST`.

Impact:
- Mostly normal UI defaulting, but still a hardcoded operational choice.

## Test-Only

### 1. Frontend E2E tests use seeded demo credentials
- Files:
  - `frontend/tests/e2e/helpers.ts`
  - `frontend/tests/e2e/auth.spec.ts`
  - `frontend/tests/e2e/admin.spec.ts`

Finding:
- Playwright helpers and tests hardcode the seeded demo users and `Demo123!`.

Impact:
- Test-only.
- Coupled to seed data remaining stable.

### 2. Frontend test fixtures include fixed upload assets
- Files:
  - `frontend/tests/fixtures/valid-bulk-import.csv`
  - `frontend/tests/fixtures/sample-upload.pdf`
  - `frontend/tests/e2e/loan-workflow.spec.ts`

Finding:
- Bulk import and upload flows depend on static fixture files.

Impact:
- Normal test fixture usage.

### 3. Backend unit tests use Mockito mocks extensively
- Files include:
  - `src/test/java/com/covenantiq/service/AuthServiceTest.java`
  - `src/test/java/com/covenantiq/service/AlertServiceTest.java`
  - `src/test/java/com/covenantiq/service/WorkflowServiceTest.java`
  - `src/test/java/com/covenantiq/service/ChangeControlServiceTest.java`
  - `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java`
  - `src/test/java/com/covenantiq/service/PortfolioSummaryServiceTest.java`
  - `src/test/java/com/covenantiq/service/TrendAnalysisServiceTest.java`
  - `src/test/java/com/covenantiq/service/CovenantEvaluationServiceTest.java`
  - `src/test/java/com/covenantiq/service/ExportServiceTest.java`

Finding:
- Standard unit-test mocks are used via `@Mock` and `Mockito.mock(...)`.

Impact:
- Expected test-only behavior.

### 4. Backend integration tests use mock multipart payloads
- Files:
  - `src/test/java/com/covenantiq/integration/AttachmentIntegrationTest.java`
  - `src/test/java/com/covenantiq/integration/BulkImportIntegrationTest.java`

Finding:
- `MockMultipartFile` is used for upload integration tests.

Impact:
- Expected test-only behavior.

### 5. Mockito test runtime is explicitly configured
- File: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

Finding:
- Mockito mock maker is explicitly configured as `mock-maker-subclass`.

Impact:
- Test-only infrastructure.

## Cleanup Priority

### Highest Priority
- Remove hardcoded runtime secrets from `application.yml`.
- Remove or hard-gate the prefilled demo login credentials.
- Replace the partly fake portfolio chart with either real data or an explicit empty/demo state.

### Medium Priority
- Move hardcoded workflow bootstrap behavior behind explicit environment/bootstrap controls.
- Remove canned change-request creation from runtime UI, or mark it clearly as demo-only.
- Replace static report date defaults with relative dates or empty inputs.

### Lower Priority
- Move covenant templates and other starter values into managed configuration if they are intended to be governed.
- Keep test fixtures and Mockito usage as-is unless test strategy changes.

## Recommended Next Actions

1. Split config by environment so local/demo defaults do not masquerade as production defaults.
2. Add an explicit `demo mode` flag for seeded accounts, prefilled forms, and sample governance screens.
3. Remove mixed real/fake analytics from runtime dashboards.
4. Decouple automated tests from shared demo credentials where practical.
