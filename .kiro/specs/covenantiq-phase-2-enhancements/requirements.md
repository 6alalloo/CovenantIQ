# Requirements Document

## Introduction

CovenantIQ is a comprehensive commercial loan covenant monitoring system that helps credit risk analysts monitor loan portfolios for covenant compliance and detect early warning signals of financial distress. This document covers both Phase 1 (implemented) and Phase 2 (planned enhancements) requirements.

The system serves financial analysts, risk leads, and administrators who monitor commercial loan portfolios for covenant compliance and early warning signals of financial distress.

### Phase 1 (Implemented)
Phase 1 delivered core loan management, covenant evaluation, trend analysis, and risk monitoring capabilities with a React frontend and Java Spring Boot backend deployed as a single Docker container.

### Phase 2 (Planned)
Phase 2 expands the system with enhanced risk analysis, alert lifecycle management, JWT authentication, role-based access control, portfolio-wide analytics, bulk import, document attachments, and collaboration features.

## Glossary

- **CovenantIQ_System**: The commercial loan covenant monitoring application
- **Risk_Details_API**: REST endpoint providing covenant-level risk breakdown
- **Alert_Manager**: Component managing alert lifecycle and status transitions
- **Export_Service**: Component generating CSV exports of alerts and covenant results
- **Covenant_Evaluator**: Component calculating financial ratios and evaluating covenant compliance
- **Bulk_Import_Service**: Component processing multi-statement CSV/Excel uploads
- **Portfolio_Aggregator**: Component computing portfolio-wide risk metrics
- **Auth_Service**: Component managing JWT-based authentication
- **Authorization_Service**: Component enforcing role-based access control
- **Document_Store**: Component managing PDF attachments as database blobs
- **Activity_Logger**: Component recording user actions and business events
- **Early_Warning_Detector**: Component identifying risk patterns in financial trends
- **Financial_Statement**: Record of a borrower's financial position at a point in time
- **Covenant_Result**: Outcome of evaluating a covenant against a financial statement
- **Alert**: Notification generated when a covenant breach or early warning condition is detected
- **Analyst**: User role with full CRUD permissions on loans and covenants
- **Risk_Lead**: User role with read-only access plus alert resolution capabilities
- **Admin**: User role with user management capabilities
- **Active_Loan**: A loan with status not equal to CLOSED or PAID_OFF
- **Correlation_ID**: Unique identifier linking related log entries across a request

## Requirements

---

## Phase 1 Requirements (Implemented)

### Requirement P1-1: Loan Creation

**User Story:** As a financial analyst, I want to create loan records with borrower information, so that I can track covenant compliance for each loan.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/loans with valid loan data, THE CovenantIQ_System SHALL create loan record and return HTTP 201 with loan details
2. THE CovenantIQ_System SHALL require fields: borrowerName, principalAmount, startDate
3. THE CovenantIQ_System SHALL set initial loan status to ACTIVE
4. THE CovenantIQ_System SHALL validate principalAmount is greater than zero
5. THE CovenantIQ_System SHALL validate startDate is not in the future
6. WHEN invalid data is provided, THE CovenantIQ_System SHALL return HTTP 400 with RFC7807 error response

### Requirement P1-2: Loan Listing and Retrieval

**User Story:** As a financial analyst, I want to view all loans and retrieve individual loan details, so that I can monitor my portfolio.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans, THE CovenantIQ_System SHALL return HTTP 200 with paginated list of loans
2. THE CovenantIQ_System SHALL support pagination parameters: page, size, sort
3. THE CovenantIQ_System SHALL default to page size of 20 if not specified
4. WHEN a GET request is made to /api/v1/loans/{id} with valid loan ID, THE CovenantIQ_System SHALL return HTTP 200 with loan details
5. WHEN a GET request is made to /api/v1/loans/{id} with non-existent loan ID, THE CovenantIQ_System SHALL return HTTP 404 with RFC7807 error response

### Requirement P1-3: Loan Closure

**User Story:** As a financial analyst, I want to close loans that have been paid off or terminated, so that they are excluded from active monitoring.

#### Acceptance Criteria

1. WHEN a PATCH request is made to /api/v1/loans/{id}/close with valid loan ID, THE CovenantIQ_System SHALL update loan status to CLOSED and return HTTP 200
2. WHEN attempting to close an already CLOSED loan, THE CovenantIQ_System SHALL return HTTP 409 with conflict error
3. WHEN a loan is CLOSED, THE CovenantIQ_System SHALL prevent new covenant creation with HTTP 409 error
4. WHEN a loan is CLOSED, THE CovenantIQ_System SHALL prevent new financial statement submission with HTTP 409 error
5. THE CovenantIQ_System SHALL continue to display historical data for CLOSED loans

### Requirement P1-4: Covenant Creation

**User Story:** As a financial analyst, I want to add covenants to loans with specific thresholds and comparison operators, so that I can monitor compliance requirements.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/loans/{loanId}/covenants with valid covenant data, THE CovenantIQ_System SHALL create covenant and return HTTP 201
2. THE CovenantIQ_System SHALL require fields: covenantType, thresholdValue, comparisonType, severityLevel
3. THE CovenantIQ_System SHALL support covenant types: CURRENT_RATIO, DEBT_TO_EQUITY
4. THE CovenantIQ_System SHALL support comparison types: GREATER_THAN_EQUAL, LESS_THAN_EQUAL
5. THE CovenantIQ_System SHALL support severity levels: LOW, MEDIUM, HIGH
6. THE CovenantIQ_System SHALL enforce unique constraint on (loan_id, covenant_type) returning HTTP 409 on duplicate
7. WHEN attempting to add covenant to CLOSED loan, THE CovenantIQ_System SHALL return HTTP 409 with conflict error

### Requirement P1-5: Financial Statement Submission

**User Story:** As a financial analyst, I want to submit quarterly and annual financial statements for loans, so that covenant compliance can be evaluated.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/loans/{loanId}/financial-statements with valid statement data, THE CovenantIQ_System SHALL create or update statement and return HTTP 201
2. THE CovenantIQ_System SHALL require fields: periodType, fiscalYear, currentAssets, currentLiabilities, totalDebt, totalEquity
3. THE CovenantIQ_System SHALL support period types: QUARTERLY, ANNUAL
4. WHEN periodType is QUARTERLY, THE CovenantIQ_System SHALL require fiscalQuarter field (1-4)
5. WHEN periodType is ANNUAL, THE CovenantIQ_System SHALL allow fiscalQuarter to be null
6. WHEN duplicate period statement exists, THE CovenantIQ_System SHALL supersede prior version by setting superseded flag to true
7. WHEN a statement is superseded, THE CovenantIQ_System SHALL also mark related CovenantResults and Alerts as superseded
8. WHEN attempting to submit statement for CLOSED loan, THE CovenantIQ_System SHALL return HTTP 409 with conflict error
9. THE CovenantIQ_System SHALL validate all monetary fields are non-negative
10. THE CovenantIQ_System SHALL record submissionTimestampUtc in UTC timezone

### Requirement P1-6: Current Ratio Calculation

**User Story:** As a financial analyst, I want the system to calculate Current Ratio from financial statements, so that liquidity covenants can be evaluated.

#### Acceptance Criteria

1. WHEN calculating Current Ratio, THE FinancialRatioService SHALL compute currentAssets divided by currentLiabilities
2. THE FinancialRatioService SHALL use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places
3. WHEN currentLiabilities is zero, THE FinancialRatioService SHALL throw validation exception with HTTP 422 error
4. THE FinancialRatioService SHALL return calculated ratio as BigDecimal

### Requirement P1-7: Debt-to-Equity Ratio Calculation

**User Story:** As a financial analyst, I want the system to calculate Debt-to-Equity ratio from financial statements, so that leverage covenants can be evaluated.

#### Acceptance Criteria

1. WHEN calculating Debt-to-Equity ratio, THE FinancialRatioService SHALL compute totalDebt divided by totalEquity
2. THE FinancialRatioService SHALL use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places
3. WHEN totalEquity is zero, THE FinancialRatioService SHALL throw validation exception with HTTP 422 error
4. THE FinancialRatioService SHALL return calculated ratio as BigDecimal

### Requirement P1-8: Covenant Evaluation

**User Story:** As a financial analyst, I want covenants to be automatically evaluated when financial statements are submitted, so that I can identify compliance issues immediately.

#### Acceptance Criteria

1. WHEN a financial statement is submitted, THE CovenantEvaluationService SHALL evaluate all active covenants for that loan
2. THE CovenantEvaluationService SHALL calculate the appropriate ratio based on covenant type
3. THE CovenantEvaluationService SHALL compare calculated value against threshold using specified comparison operator
4. WHEN comparison operator is GREATER_THAN_EQUAL and calculated value is greater than or equal to threshold, THE CovenantEvaluationService SHALL set status to PASS
5. WHEN comparison operator is LESS_THAN_EQUAL and calculated value is less than or equal to threshold, THE CovenantEvaluationService SHALL set status to PASS
6. WHEN covenant condition is not met, THE CovenantEvaluationService SHALL set status to BREACH
7. THE CovenantEvaluationService SHALL persist CovenantResult with fields: covenant, financialStatement, actualValue, status, evaluationTimestampUtc
8. THE CovenantEvaluationService SHALL execute within a transaction that rolls back on any failure

### Requirement P1-9: Breach Alert Generation

**User Story:** As a financial analyst, I want to receive alerts when covenants are breached, so that I can take immediate action.

#### Acceptance Criteria

1. WHEN a covenant evaluation results in BREACH status, THE CovenantEvaluationService SHALL create Alert with alertType BREACH
2. THE Alert SHALL include fields: loan, alertType, message, severityLevel, triggeredTimestampUtc, financialStatement
3. THE Alert message SHALL describe which covenant was breached and by how much
4. THE Alert severityLevel SHALL match the covenant's severityLevel
5. THE Alert SHALL be persisted in the same transaction as the CovenantResult

### Requirement P1-10: Consecutive Decline Detection

**User Story:** As a risk lead, I want to detect when Current Ratio declines for 3 consecutive periods, so that I can identify deteriorating financial health before covenant breach.

#### Acceptance Criteria

1. WHEN a financial statement is submitted, THE TrendAnalysisService SHALL check for 3 consecutive Current Ratio declines
2. THE TrendAnalysisService SHALL evaluate declines separately for QUARTERLY and ANNUAL cadence streams
3. WHEN 3 consecutive declines are detected, THE TrendAnalysisService SHALL create Alert with alertType EARLY_WARNING
4. THE Alert message SHALL indicate "3 consecutive Current Ratio declines detected"
5. THE Alert severityLevel SHALL be MEDIUM
6. THE TrendAnalysisService SHALL require minimum 3 historical statements before detecting consecutive declines
7. THE TrendAnalysisService SHALL use BigDecimal comparison for decline detection

### Requirement P1-11: Near-Threshold Warning Detection

**User Story:** As a risk lead, I want to receive warnings when ratios approach covenant thresholds, so that I can intervene before actual breach.

#### Acceptance Criteria

1. WHEN a financial statement is submitted, THE TrendAnalysisService SHALL check if calculated ratios are within 5% of covenant thresholds
2. THE TrendAnalysisService SHALL only trigger warning when ratio is moving toward breach direction
3. WHEN GREATER_THAN_EQUAL covenant has ratio within 5% below threshold, THE TrendAnalysisService SHALL create EARLY_WARNING alert
4. WHEN LESS_THAN_EQUAL covenant has ratio within 5% above threshold, THE TrendAnalysisService SHALL create EARLY_WARNING alert
5. THE Alert message SHALL indicate "Approaching threshold" with specific covenant details
6. THE Alert severityLevel SHALL be LOW
7. THE TrendAnalysisService SHALL use BigDecimal arithmetic for threshold proximity calculation

### Requirement P1-12: Covenant Results Retrieval

**User Story:** As a financial analyst, I want to view historical covenant evaluation results for a loan, so that I can track compliance trends over time.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{loanId}/covenant-results, THE CovenantIQ_System SHALL return HTTP 200 with paginated list of covenant results
2. THE CovenantIQ_System SHALL exclude superseded results by default
3. THE CovenantIQ_System SHALL support pagination parameters: page, size, sort
4. THE CovenantIQ_System SHALL sort results by evaluationTimestampUtc descending by default
5. THE CovenantIQ_System SHALL include covenant details and financial statement reference in response

### Requirement P1-13: Alert Retrieval

**User Story:** As a financial analyst, I want to view all alerts for a loan, so that I can understand risk events and warnings.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{loanId}/alerts, THE CovenantIQ_System SHALL return HTTP 200 with paginated list of alerts
2. THE CovenantIQ_System SHALL exclude superseded alerts by default
3. THE CovenantIQ_System SHALL support pagination parameters: page, size, sort
4. THE CovenantIQ_System SHALL sort alerts by triggeredTimestampUtc descending by default
5. THE CovenantIQ_System SHALL include alert type, severity, message, and triggered timestamp in response

### Requirement P1-14: Risk Summary Calculation

**User Story:** As a financial analyst, I want to view a consolidated risk summary for each loan, so that I can quickly assess overall loan health.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{loanId}/risk-summary, THE RiskSummaryService SHALL return HTTP 200 with risk metrics
2. THE RiskSummaryService SHALL include metrics: totalCovenants, breachedCovenants, activeWarnings, overallRiskLevel
3. THE RiskSummaryService SHALL count only non-superseded covenant results from latest evaluation cycle
4. THE RiskSummaryService SHALL count only non-superseded EARLY_WARNING alerts from latest evaluation cycle in activeWarnings
5. THE RiskSummaryService SHALL set overallRiskLevel to HIGH if any HIGH severity breach exists
6. THE RiskSummaryService SHALL set overallRiskLevel to MEDIUM if any breach or warning exists without HIGH severity breach
7. THE RiskSummaryService SHALL set overallRiskLevel to LOW if no breaches or warnings exist
8. THE RiskSummaryService SHALL compute results within 500ms

### Requirement P1-15: RFC7807 Error Handling

**User Story:** As a frontend developer, I want consistent error responses following RFC7807 standard, so that I can display meaningful error messages to users.

#### Acceptance Criteria

1. WHEN any API error occurs, THE CovenantIQ_System SHALL return response with Content-Type application/problem+json
2. THE error response SHALL include fields: type, title, status, detail, instance
3. WHEN resource is not found, THE CovenantIQ_System SHALL return HTTP 404 with appropriate problem details
4. WHEN business rule conflict occurs, THE CovenantIQ_System SHALL return HTTP 409 with appropriate problem details
5. WHEN validation fails, THE CovenantIQ_System SHALL return HTTP 422 with appropriate problem details including field-level errors
6. THE CovenantIQ_System SHALL include timestamp and correlation ID in error responses

### Requirement P1-16: Mock Authentication UI

**User Story:** As a demo user, I want to log in with predefined demo accounts, so that I can access the application without real authentication infrastructure.

#### Acceptance Criteria

1. THE frontend SHALL provide login page with username and password fields
2. THE frontend SHALL support demo users: analyst@demo.com, risklead@demo.com
3. WHEN valid demo credentials are entered, THE frontend SHALL store session in browser storage and redirect to dashboard
4. WHEN invalid credentials are entered, THE frontend SHALL display error message
5. THE frontend SHALL implement route guards preventing access to protected routes without session
6. THE frontend SHALL provide logout functionality clearing session storage
7. THE backend SHALL NOT enforce authentication (Phase 1 limitation)

### Requirement P1-17: Loan Management UI

**User Story:** As a financial analyst, I want to view, create, and manage loans through a web interface, so that I can efficiently manage my portfolio.

#### Acceptance Criteria

1. THE frontend SHALL display paginated loan list with columns: borrower name, principal amount, start date, status
2. THE frontend SHALL provide "Create Loan" button opening loan creation form
3. THE loan creation form SHALL validate required fields before submission
4. WHEN loan is created successfully, THE frontend SHALL display success message and refresh loan list
5. WHEN loan creation fails, THE frontend SHALL display error message from API
6. THE frontend SHALL provide loan detail view showing full loan information, covenants, statements, and alerts
7. THE frontend SHALL provide "Close Loan" button on loan detail page for ACTIVE loans

### Requirement P1-18: Covenant Management UI

**User Story:** As a financial analyst, I want to add covenants to loans through a web interface, so that I can configure monitoring rules.

#### Acceptance Criteria

1. THE frontend SHALL provide "Add Covenant" button on loan detail page
2. THE covenant form SHALL include fields: covenant type, threshold value, comparison operator, severity level
3. THE covenant form SHALL provide dropdown selections for covenant type, comparison operator, and severity level
4. THE covenant form SHALL validate threshold value is a positive number
5. WHEN covenant is created successfully, THE frontend SHALL display success message and refresh covenant list
6. WHEN covenant creation fails, THE frontend SHALL display error message from API
7. THE frontend SHALL display existing covenants in a table with columns: type, threshold, operator, severity

### Requirement P1-19: Financial Statement Submission UI

**User Story:** As a financial analyst, I want to submit financial statements through a web interface, so that covenant evaluation can be triggered.

#### Acceptance Criteria

1. THE frontend SHALL provide "Submit Statement" button on loan detail page
2. THE statement form SHALL include fields: period type, fiscal year, fiscal quarter (conditional), and all financial metrics
3. WHEN period type is QUARTERLY, THE form SHALL require fiscal quarter selection (Q1-Q4)
4. WHEN period type is ANNUAL, THE form SHALL hide fiscal quarter field
5. THE form SHALL validate all monetary fields are non-negative numbers
6. WHEN statement is submitted successfully, THE frontend SHALL display success message and refresh results/alerts
7. WHEN statement submission fails, THE frontend SHALL display error message from API

### Requirement P1-20: Risk Visualization UI

**User Story:** As a financial analyst, I want to view risk summary and trend charts for loans, so that I can quickly assess loan health and trends.

#### Acceptance Criteria

1. THE frontend SHALL display risk summary panel showing: total covenants, breached covenants, active warnings, overall risk level
2. THE risk level SHALL be color-coded: HIGH (red), MEDIUM (yellow), LOW (green)
3. THE frontend SHALL display covenant results table with pagination and sorting
4. THE frontend SHALL display alerts table with pagination and sorting
5. THE frontend SHALL provide trend chart using Recharts showing ratio values over time
6. THE trend chart SHALL support toggling between different covenant types
7. THE frontend SHALL display loading states while fetching data
8. THE frontend SHALL display error states when API calls fail

### Requirement P1-21: OpenAPI Documentation

**User Story:** As a developer, I want to access interactive API documentation, so that I can understand and test API endpoints.

#### Acceptance Criteria

1. THE CovenantIQ_System SHALL expose Swagger UI at /swagger-ui.html
2. THE Swagger UI SHALL document all API endpoints with request/response schemas
3. THE Swagger UI SHALL allow interactive testing of endpoints
4. THE OpenAPI specification SHALL include descriptions for all endpoints, parameters, and response codes
5. THE OpenAPI specification SHALL document RFC7807 error response schema

### Requirement P1-22: Demo Data Seeding

**User Story:** As a demo user, I want the system to include sample data on startup, so that I can explore features without manual data entry.

#### Acceptance Criteria

1. WHEN the application starts, THE DataInitializer SHALL create sample loans with realistic data
2. THE DataInitializer SHALL create sample covenants for demo loans
3. THE DataInitializer SHALL create sample financial statements triggering various alert conditions
4. THE demo data SHALL include examples of: passing covenants, breached covenants, consecutive declines, near-threshold warnings
5. THE DataInitializer SHALL only seed data if database is empty

### Requirement P1-23: Single Container Deployment

**User Story:** As a DevOps engineer, I want to deploy the application as a single Docker container, so that deployment is simplified.

#### Acceptance Criteria

1. THE Docker build SHALL compile backend Java application
2. THE Docker build SHALL compile frontend React application
3. THE Docker build SHALL copy frontend build artifacts into Spring Boot static resources
4. THE Docker container SHALL serve both API endpoints and frontend SPA from single process
5. THE Spring Boot application SHALL configure fallback routing to index.html for client-side routes
6. WHEN container starts, THE application SHALL be accessible on configured port (default 8080)
7. THE Docker Compose configuration SHALL allow single-command startup: docker compose up

---

## Phase 2 Requirements (Planned)

### Requirement 1: Risk Details Endpoint

**User Story:** As a financial analyst, I want to retrieve detailed risk breakdowns for a specific loan, so that I can understand which covenants are problematic and why.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{id}/risk-details with a valid loan ID, THE Risk_Details_API SHALL return HTTP 200 with covenant-level breakdown from the latest evaluation cycle
2. THE Risk_Details_API SHALL include for each covenant: covenant name, current value, threshold value, compliance status, reason string, and triggered rule names
3. WHEN a GET request is made to /api/v1/loans/{id}/risk-details with a non-existent loan ID, THE Risk_Details_API SHALL return HTTP 404 with RFC7807 error response
4. WHEN a GET request is made to /api/v1/loans/{id}/risk-details for a loan with no evaluation results, THE Risk_Details_API SHALL return HTTP 200 with an empty covenant list
5. THE Risk_Details_API SHALL return results within 500ms for loans with up to 20 covenants

### Requirement 2: Alert Status Management

**User Story:** As a risk lead, I want to track the lifecycle of alerts from detection through resolution, so that I can manage my team's workload and maintain audit trails.

#### Acceptance Criteria

1. THE Alert_Manager SHALL support alert statuses: OPEN, ACKNOWLEDGED, UNDER_REVIEW, RESOLVED
2. WHEN an alert is created, THE Alert_Manager SHALL set initial status to OPEN
3. WHEN a PATCH request is made to /api/v1/alerts/{id}/status with valid status transition, THE Alert_Manager SHALL update the alert status and return HTTP 200
4. THE Alert_Manager SHALL record acknowledgedBy, acknowledgedAt, resolvedBy, resolvedAt, and resolutionNotes fields during status transitions
5. WHEN an alert status is changed to ACKNOWLEDGED, THE Alert_Manager SHALL record the user ID and timestamp in acknowledgedBy and acknowledgedAt fields
6. WHEN an alert status is changed to RESOLVED, THE Alert_Manager SHALL record the user ID, timestamp, and resolution notes in resolvedBy, resolvedAt, and resolutionNotes fields
7. WHEN calculating active warnings count, THE Alert_Manager SHALL exclude alerts with status RESOLVED
8. THE Alert_Manager SHALL sort alerts by severity descending, then by creation timestamp ascending

### Requirement 3: Alert Resolution Tracking

**User Story:** As an analyst, I want to document how alerts were resolved, so that future similar situations can be handled consistently.

#### Acceptance Criteria

1. WHEN a PATCH request to /api/v1/alerts/{id}/status includes resolutionNotes, THE Alert_Manager SHALL store the notes with maximum length 2000 characters
2. WHEN a PATCH request attempts to set status to RESOLVED without resolutionNotes, THE Alert_Manager SHALL return HTTP 400 with validation error
3. THE Alert_Manager SHALL prevent status transitions from RESOLVED back to OPEN, ACKNOWLEDGED, or UNDER_REVIEW
4. WHEN retrieving alert details, THE Alert_Manager SHALL include all resolution tracking fields in the response

### Requirement 4: CSV Export for Alerts

**User Story:** As a financial analyst, I want to export alerts to CSV format, so that I can analyze them in spreadsheet tools or share with stakeholders.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{loanId}/alerts/export, THE Export_Service SHALL return a CSV file with Content-Type text/csv
2. THE Export_Service SHALL include CSV headers: Alert ID, Loan ID, Loan Name, Severity, Message, Status, Created At, Acknowledged By, Acknowledged At, Resolved By, Resolved At, Resolution Notes
3. THE Export_Service SHALL format timestamps in ISO 8601 format
4. THE Export_Service SHALL escape commas and quotes within field values according to RFC 4180
5. WHEN a loan has no alerts, THE Export_Service SHALL return a CSV file with headers only

### Requirement 5: CSV Export for Covenant Results

**User Story:** As a financial analyst, I want to export covenant evaluation results to CSV format, so that I can perform trend analysis in external tools.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/loans/{loanId}/covenant-results/export, THE Export_Service SHALL return a CSV file with Content-Type text/csv
2. THE Export_Service SHALL include CSV headers: Evaluation Date, Covenant Name, Covenant Type, Calculated Value, Threshold Value, Comparison Operator, Compliance Status, Statement Period End Date
3. THE Export_Service SHALL format decimal values with 4 decimal places
4. THE Export_Service SHALL sort results by evaluation date descending, then by covenant name ascending
5. WHEN a loan has no covenant results, THE Export_Service SHALL return a CSV file with headers only

### Requirement 6: Additional Covenant Types

**User Story:** As a financial analyst, I want to evaluate loans against industry-standard covenant types beyond Current Ratio and Debt-to-Equity, so that I can monitor comprehensive financial health indicators.

#### Acceptance Criteria

1. THE Covenant_Evaluator SHALL support covenant types: CURRENT_RATIO, DEBT_TO_EQUITY, DSCR, INTEREST_COVERAGE, TANGIBLE_NET_WORTH, DEBT_TO_EBITDA, FIXED_CHARGE_COVERAGE, QUICK_RATIO
2. WHEN evaluating DSCR covenant, THE Covenant_Evaluator SHALL calculate (Net Operating Income) divided by (Total Debt Service)
3. WHEN evaluating INTEREST_COVERAGE covenant, THE Covenant_Evaluator SHALL calculate (EBIT) divided by (Interest Expense)
4. WHEN evaluating TANGIBLE_NET_WORTH covenant, THE Covenant_Evaluator SHALL calculate (Total Assets minus Intangible Assets minus Total Liabilities)
5. WHEN evaluating DEBT_TO_EBITDA covenant, THE Covenant_Evaluator SHALL calculate (Total Debt) divided by (EBITDA)
6. WHEN evaluating FIXED_CHARGE_COVERAGE covenant, THE Covenant_Evaluator SHALL calculate (EBIT plus Fixed Charges) divided by (Fixed Charges plus Interest Expense)
7. WHEN evaluating QUICK_RATIO covenant, THE Covenant_Evaluator SHALL calculate (Current Assets minus Inventory) divided by (Current Liabilities)
8. THE Covenant_Evaluator SHALL use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places for all calculations
9. WHEN a required field for covenant calculation is missing from Financial_Statement, THE Covenant_Evaluator SHALL return HTTP 400 with descriptive error indicating missing field

### Requirement 7: Financial Statement Schema Extension

**User Story:** As a system administrator, I want the financial statement entity to capture all fields required for expanded covenant types, so that covenant calculations have necessary data.

#### Acceptance Criteria

1. THE CovenantIQ_System SHALL extend Financial_Statement entity to include fields: netOperatingIncome, totalDebtService, ebit, interestExpense, intangibleAssets, ebitda, fixedCharges, inventory
2. THE CovenantIQ_System SHALL validate that all monetary fields accept values with up to 2 decimal places
3. THE CovenantIQ_System SHALL validate that netOperatingIncome, totalDebtService, ebit, interestExpense, ebitda, fixedCharges are greater than or equal to zero
4. THE CovenantIQ_System SHALL allow intangibleAssets and inventory to be zero or positive

### Requirement 8: Bulk Financial Statement Import

**User Story:** As a financial analyst, I want to upload multiple financial statements at once via CSV or Excel, so that I can quickly onboard loans with historical data.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/loans/{loanId}/financial-statements/bulk-import with valid CSV file, THE Bulk_Import_Service SHALL process all rows and return HTTP 200 with import summary
2. THE Bulk_Import_Service SHALL accept CSV files with headers: periodEndDate, totalAssets, totalLiabilities, currentAssets, currentLiabilities, totalDebt, totalEquity, netOperatingIncome, totalDebtService, ebit, interestExpense, intangibleAssets, ebitda, fixedCharges, inventory
3. THE Bulk_Import_Service SHALL accept Excel files with .xlsx extension containing the same column structure
4. THE Bulk_Import_Service SHALL validate each row and report which rows succeeded and which failed with specific error messages
5. THE Bulk_Import_Service SHALL return import summary containing: totalRows, successCount, failureCount, and array of row-level results with row number and error message
6. WHEN a row fails validation, THE Bulk_Import_Service SHALL continue processing remaining rows
7. THE Bulk_Import_Service SHALL reject files larger than 5MB with HTTP 413 error
8. THE Bulk_Import_Service SHALL parse dates in ISO 8601 format (YYYY-MM-DD)
9. WHEN duplicate periodEndDate exists for the same loan, THE Bulk_Import_Service SHALL mark that row as failed with duplicate error message

### Requirement 9: Portfolio-Wide Risk Aggregation

**User Story:** As a risk lead, I want to view aggregated risk metrics across my entire loan portfolio, so that I can identify systemic issues and prioritize resources.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/portfolio/summary, THE Portfolio_Aggregator SHALL return HTTP 200 with portfolio-wide metrics
2. THE Portfolio_Aggregator SHALL include metrics: totalActiveLoans, totalBreaches, highRiskLoanCount, mediumRiskLoanCount, lowRiskLoanCount, totalOpenAlerts, totalUnderReviewAlerts
3. THE Portfolio_Aggregator SHALL count only Active_Loan records in totalActiveLoans
4. THE Portfolio_Aggregator SHALL define highRiskLoanCount as loans with 2 or more covenant breaches in latest evaluation cycle
5. THE Portfolio_Aggregator SHALL define mediumRiskLoanCount as loans with 1 covenant breach in latest evaluation cycle
6. THE Portfolio_Aggregator SHALL define lowRiskLoanCount as Active_Loan records with zero breaches in latest evaluation cycle
7. THE Portfolio_Aggregator SHALL count only alerts with status OPEN or UNDER_REVIEW in totalOpenAlerts and totalUnderReviewAlerts
8. THE Portfolio_Aggregator SHALL compute results within 2 seconds for portfolios with up to 1000 active loans

### Requirement 10: JWT Authentication

**User Story:** As a system administrator, I want users to authenticate with username and password receiving JWT tokens, so that the system has secure session management.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/auth/login with valid username and password, THE Auth_Service SHALL return HTTP 200 with JWT access token and refresh token
2. THE Auth_Service SHALL generate JWT access tokens with 1 hour expiration
3. THE Auth_Service SHALL generate JWT refresh tokens with 7 day expiration
4. THE Auth_Service SHALL include user ID, username, and roles in JWT claims
5. WHEN a POST request is made to /api/v1/auth/login with invalid credentials, THE Auth_Service SHALL return HTTP 401 with error message
6. WHEN a POST request is made to /api/v1/auth/refresh with valid refresh token, THE Auth_Service SHALL return HTTP 200 with new access token
7. THE Auth_Service SHALL sign JWT tokens using HS256 algorithm with secret key stored in application configuration
8. WHEN a request is made to protected endpoints without valid JWT token, THE CovenantIQ_System SHALL return HTTP 401 with WWW-Authenticate header

### Requirement 11: Password Security

**User Story:** As a system administrator, I want user passwords to be securely stored and validated, so that unauthorized access is prevented.

#### Acceptance Criteria

1. THE Auth_Service SHALL hash passwords using BCrypt with cost factor 12 before storing in database
2. THE Auth_Service SHALL enforce minimum password length of 8 characters
3. THE Auth_Service SHALL require passwords to contain at least one uppercase letter, one lowercase letter, one digit, and one special character
4. WHEN a user registration or password change request fails password policy, THE Auth_Service SHALL return HTTP 400 with specific policy violation message
5. THE Auth_Service SHALL implement account lockout after 5 consecutive failed login attempts within 15 minutes
6. WHEN an account is locked, THE Auth_Service SHALL return HTTP 423 with lockout duration message

### Requirement 12: Role-Based Access Control

**User Story:** As a system administrator, I want to assign roles to users that control their permissions, so that users can only perform authorized actions.

#### Acceptance Criteria

1. THE Authorization_Service SHALL support roles: ANALYST, RISK_LEAD, ADMIN
2. THE Authorization_Service SHALL grant ANALYST role permissions: create, read, update, delete loans, covenants, financial statements, and acknowledge alerts
3. THE Authorization_Service SHALL grant RISK_LEAD role permissions: read all resources, access portfolio summary, and resolve alerts
4. THE Authorization_Service SHALL grant ADMIN role permissions: all ANALYST permissions plus create, read, update, delete users
5. WHEN a user attempts an action not permitted by their role, THE Authorization_Service SHALL return HTTP 403 with forbidden error
6. THE Authorization_Service SHALL allow users to have multiple roles with combined permissions
7. THE Authorization_Service SHALL validate role-based permissions on all protected endpoints before processing requests

### Requirement 13: User Management

**User Story:** As an administrator, I want to create, update, and deactivate user accounts, so that I can manage system access.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/users with valid user data, THE CovenantIQ_System SHALL create user account and return HTTP 201 with user details excluding password
2. THE CovenantIQ_System SHALL require username, password, email, and at least one role during user creation
3. WHEN a GET request is made to /api/v1/users, THE CovenantIQ_System SHALL return HTTP 200 with list of all users excluding password fields
4. WHEN a PATCH request is made to /api/v1/users/{id} with role updates, THE CovenantIQ_System SHALL update user roles and return HTTP 200
5. WHEN a DELETE request is made to /api/v1/users/{id}, THE CovenantIQ_System SHALL deactivate the user account and return HTTP 204
6. THE CovenantIQ_System SHALL prevent deletion of the last ADMIN user with HTTP 400 error
7. THE CovenantIQ_System SHALL enforce unique username constraint returning HTTP 409 on duplicate username

### Requirement 14: Document Attachment Storage

**User Story:** As a financial analyst, I want to attach PDF financial statements to financial statement records, so that I can reference source documents during covenant reviews.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/financial-statements/{id}/attachments with PDF file, THE Document_Store SHALL store file as database blob and return HTTP 201 with attachment metadata
2. THE Document_Store SHALL accept PDF files with maximum size 10MB
3. THE Document_Store SHALL store attachment metadata: filename, fileSize, contentType, uploadedBy, uploadedAt
4. WHEN a GET request is made to /api/v1/financial-statements/{id}/attachments, THE Document_Store SHALL return HTTP 200 with list of attachment metadata
5. WHEN a GET request is made to /api/v1/financial-statements/{id}/attachments/{attachmentId}, THE Document_Store SHALL return HTTP 200 with PDF file and Content-Type application/pdf
6. WHEN a DELETE request is made to /api/v1/financial-statements/{id}/attachments/{attachmentId}, THE Document_Store SHALL remove attachment and return HTTP 204
7. THE Document_Store SHALL reject non-PDF files with HTTP 415 unsupported media type error
8. WHEN a financial statement is deleted, THE Document_Store SHALL cascade delete all associated attachments

### Requirement 15: Loan Comments and Notes

**User Story:** As a financial analyst, I want to add comments and notes to loan records, so that I can share context and observations with my team.

#### Acceptance Criteria

1. WHEN a POST request is made to /api/v1/loans/{loanId}/comments with comment text, THE CovenantIQ_System SHALL create comment and return HTTP 201
2. THE CovenantIQ_System SHALL record comment metadata: commentText, createdBy, createdAt
3. THE CovenantIQ_System SHALL enforce maximum comment length of 5000 characters
4. WHEN a GET request is made to /api/v1/loans/{loanId}/comments, THE CovenantIQ_System SHALL return HTTP 200 with comments sorted by createdAt descending
5. WHEN a DELETE request is made to /api/v1/loans/{loanId}/comments/{commentId}, THE CovenantIQ_System SHALL allow deletion only by comment creator or ADMIN role
6. THE CovenantIQ_System SHALL include commenter username and timestamp in comment response

### Requirement 16: Activity Logging

**User Story:** As a risk lead, I want to view an audit trail of actions taken on loans, so that I can understand the history of decisions and changes.

#### Acceptance Criteria

1. THE Activity_Logger SHALL record events: LOAN_CREATED, LOAN_UPDATED, COVENANT_CREATED, COVENANT_UPDATED, STATEMENT_SUBMITTED, ALERT_ACKNOWLEDGED, ALERT_RESOLVED, COMMENT_ADDED
2. THE Activity_Logger SHALL store activity metadata: eventType, entityType, entityId, userId, username, timestamp, description
3. WHEN a GET request is made to /api/v1/loans/{loanId}/activity, THE CovenantIQ_System SHALL return HTTP 200 with activity log entries sorted by timestamp descending
4. THE Activity_Logger SHALL generate human-readable descriptions such as "John Doe acknowledged alert #123" or "Jane Smith submitted Q4 2023 financial statement"
5. THE Activity_Logger SHALL retain activity logs for minimum 90 days
6. WHEN a GET request is made to /api/v1/activity with date range parameters, THE CovenantIQ_System SHALL return filtered activity across all loans

### Requirement 17: Structured Logging with Correlation IDs

**User Story:** As a system administrator, I want structured JSON logs with correlation IDs, so that I can trace requests across components and troubleshoot issues efficiently.

#### Acceptance Criteria

1. THE CovenantIQ_System SHALL output logs in JSON format with fields: timestamp, level, logger, message, correlationId, userId, requestPath, responseStatus
2. THE CovenantIQ_System SHALL generate unique Correlation_ID for each incoming HTTP request
3. THE CovenantIQ_System SHALL include Correlation_ID in all log entries related to that request
4. THE CovenantIQ_System SHALL return Correlation_ID in X-Correlation-ID response header
5. THE CovenantIQ_System SHALL log business events at INFO level: covenant breach detected, alert created, alert resolved, bulk import completed
6. THE CovenantIQ_System SHALL log errors at ERROR level with stack traces
7. THE CovenantIQ_System SHALL log authentication failures at WARN level with username and IP address

### Requirement 18: Health Check Endpoint

**User Story:** As a DevOps engineer, I want a health check endpoint, so that I can monitor application availability and integrate with orchestration tools.

#### Acceptance Criteria

1. WHEN a GET request is made to /actuator/health, THE CovenantIQ_System SHALL return HTTP 200 with status UP when application is healthy
2. THE CovenantIQ_System SHALL include component health checks: database connectivity, disk space availability
3. WHEN database connection fails, THE CovenantIQ_System SHALL return HTTP 503 with status DOWN and component details
4. THE CovenantIQ_System SHALL respond to health check requests within 1 second
5. THE CovenantIQ_System SHALL not require authentication for /actuator/health endpoint

### Requirement 19: Volatility Detection

**User Story:** As a risk lead, I want to detect large swings in financial ratios, so that I can identify borrowers experiencing financial instability even if covenants are not breached.

#### Acceptance Criteria

1. WHEN evaluating financial statements, THE Early_Warning_Detector SHALL calculate ratio volatility as standard deviation over last 4 periods
2. WHEN ratio volatility exceeds 0.3 for any covenant, THE Early_Warning_Detector SHALL create alert with severity MEDIUM and message indicating high volatility
3. THE Early_Warning_Detector SHALL require minimum 4 financial statement periods before calculating volatility
4. THE Early_Warning_Detector SHALL use BigDecimal arithmetic for volatility calculations with HALF_UP rounding to 4 decimal places
5. WHEN insufficient historical data exists, THE Early_Warning_Detector SHALL skip volatility detection without error

### Requirement 20: Seasonal Anomaly Detection

**User Story:** As a financial analyst, I want to detect when current ratios deviate significantly from historical seasonal patterns, so that I can identify unusual financial performance.

#### Acceptance Criteria

1. WHEN evaluating financial statements, THE Early_Warning_Detector SHALL compare current ratio to same quarter in previous year
2. WHEN current ratio deviates by more than 25 percent from same quarter previous year, THE Early_Warning_Detector SHALL create alert with severity LOW and message indicating seasonal anomaly
3. THE Early_Warning_Detector SHALL require minimum 4 quarters of historical data before performing seasonal anomaly detection
4. THE Early_Warning_Detector SHALL calculate percentage deviation as absolute value of ((current minus historical) divided by historical) times 100
5. WHEN insufficient historical data exists, THE Early_Warning_Detector SHALL skip seasonal anomaly detection without error

### Requirement 21: Parser for CSV Financial Statement Import

**User Story:** As a developer, I want to parse CSV files containing financial statement data, so that the bulk import feature can process uploaded files.

#### Acceptance Criteria

1. WHEN a valid CSV file is provided, THE Bulk_Import_Service SHALL parse it into a list of FinancialStatementDTO objects
2. WHEN an invalid CSV file is provided with malformed structure, THE Bulk_Import_Service SHALL return descriptive error indicating line number and parsing issue
3. THE Bulk_Import_Service SHALL format FinancialStatementDTO objects back into valid CSV files for export functionality
4. FOR ALL valid FinancialStatementDTO objects, parsing then formatting then parsing SHALL produce equivalent objects with same field values

### Requirement 22: Parser for Excel Financial Statement Import

**User Story:** As a developer, I want to parse Excel files containing financial statement data, so that analysts can upload data in their preferred format.

#### Acceptance Criteria

1. WHEN a valid Excel file with .xlsx extension is provided, THE Bulk_Import_Service SHALL parse it into a list of FinancialStatementDTO objects
2. WHEN an invalid Excel file is provided with missing required columns, THE Bulk_Import_Service SHALL return descriptive error indicating missing column names
3. THE Bulk_Import_Service SHALL read data from the first worksheet only
4. THE Bulk_Import_Service SHALL skip empty rows without generating errors
5. FOR ALL valid Excel files, parsing then converting to CSV then parsing SHALL produce equivalent FinancialStatementDTO objects

