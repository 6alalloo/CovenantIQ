# Implementation Plan: CovenantIQ Phase 1 & Phase 2

## Overview

This task list covers both Phase 1 (completed) and Phase 2 (planned) implementation for CovenantIQ, a commercial loan covenant monitoring system. Phase 1 delivered core loan management, covenant evaluation, and risk monitoring. Phase 2 adds enterprise features including JWT authentication, role-based access control, enhanced covenant types, alert lifecycle management, portfolio analytics, bulk import, document attachments, and collaboration features.

**Technology Stack:** Java 21, Spring Boot 3.x, React 18 with TypeScript, H2 Database, Docker

**Phase 1 Status:** All tasks marked [x] are completed and tested.
**Phase 2 Status:** All tasks marked [ ] are planned for implementation.

---

## Phase 1 Tasks (COMPLETED)

### [x] 1. Foundation & Project Setup
- [x] 1.1 Initialize Spring Boot project with Java 21
  - Create Maven project with Spring Boot 3.x parent
  - Configure application.properties with H2 database settings
  - Set up package structure: controller, service, repository, domain, dto, exception
  - _Requirements: P1-1 through P1-23_

- [x] 1.2 Initialize React frontend with TypeScript and Vite
  - Create Vite project with React 18 and TypeScript template
  - Configure Tailwind CSS for styling
  - Set up folder structure: pages, components, services, types
  - _Requirements: P1-16, P1-17_

- [x] 1.3 Configure Maven dependencies
  - Add Spring Web, Spring Data JPA, H2 Database dependencies
  - Add Jakarta Bean Validation, Springdoc OpenAPI dependencies
  - Configure Maven build to copy frontend build to Spring Boot static resources
  - _Requirements: P1-21, P1-23_

### [x] 2. Backend Domain Layer (Phase 1)
- [x] 2.1 Create enums for domain model
  - Create LoanStatus enum (ACTIVE, CLOSED, PAID_OFF)
  - Create CovenantType enum (CURRENT_RATIO, DEBT_TO_EQUITY)
  - Create ComparisonType enum (GREATER_THAN_EQUAL, LESS_THAN_EQUAL)
  - Create SeverityLevel enum (LOW, MEDIUM, HIGH)
  - Create CovenantResultStatus enum (PASS, BREACH)
  - Create AlertType enum (BREACH, EARLY_WARNING)
  - Create PeriodType enum (QUARTERLY, ANNUAL)
  - _Requirements: P1-3, P1-4, P1-5, P1-8, P1-9_

- [x] 2.2 Create Loan entity with JPA annotations
  - Define fields: id, borrowerName, principalAmount, startDate, status
  - Add @OneToMany relationships to covenants, financialStatements, alerts
  - Configure cascade operations and orphan removal
  - _Requirements: P1-1, P1-2, P1-3_

- [x] 2.3 Create Covenant entity with unique constraint
  - Define fields: id, loan, type, thresholdValue, comparisonType, severityLevel
  - Add @UniqueConstraint on (loan_id, type)
  - Use BigDecimal with precision 19, scale 6 for thresholdValue
  - _Requirements: P1-4_

- [x] 2.4 Create FinancialStatement entity
  - Define fields: id, loan, periodType, fiscalYear, fiscalQuarter
  - Add monetary fields: currentAssets, currentLiabilities, totalDebt, totalEquity, ebit, interestExpense
  - Use BigDecimal with precision 19, scale 4 for all monetary fields
  - Add submissionTimestampUtc (OffsetDateTime) and superseded flag
  - _Requirements: P1-5, P1-6, P1-7_

- [x] 2.5 Create CovenantResult entity
  - Define fields: id, covenant, financialStatement, actualValue, status, evaluationTimestampUtc, superseded
  - Use BigDecimal with precision 19, scale 4 for actualValue
  - _Requirements: P1-8, P1-12_

- [x] 2.6 Create Alert entity
  - Define fields: id, loan, financialStatement, alertType, message, severityLevel, triggeredTimestampUtc, alertRuleCode, superseded
  - Set message max length to 500 characters
  - _Requirements: P1-9, P1-10, P1-11, P1-13_

- [x] 2.7 Create repositories for all entities
  - Create LoanRepository extending JpaRepository
  - Create CovenantRepository with custom query findByLoanAndActive
  - Create FinancialStatementRepository with queries for historical data
  - Create CovenantResultRepository with query for non-superseded results
  - Create AlertRepository with query for non-superseded alerts
  - _Requirements: P1-1 through P1-13_

### [x] 3. Backend DTO Layer (Phase 1)
- [x] 3.1 Create request DTOs with validation annotations
  - Create CreateLoanRequest with @NotNull, @Positive, @PastOrPresent annotations
  - Create CreateCovenantRequest with @NotNull annotations
  - Create SubmitFinancialStatementRequest with @NotNull, @PositiveOrZero annotations
  - _Requirements: P1-1, P1-4, P1-5_

- [x] 3.2 Create response DTOs
  - Create LoanResponse with all loan fields
  - Create CovenantResponse with covenant details
  - Create FinancialStatementResponse with statement data
  - Create CovenantResultResponse with evaluation results
  - Create AlertResponse with alert details
  - Create RiskSummaryResponse with risk metrics
  - _Requirements: P1-2, P1-12, P1-13, P1-14_

### [x] 4. Backend Service Layer (Phase 1)
- [x] 4.1 Implement FinancialRatioService
  - Implement calculateCurrentRatio method (currentAssets / currentLiabilities)
  - Implement calculateDebtToEquity method (totalDebt / totalEquity)
  - Use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places
  - Throw UnprocessableEntityException for division by zero
  - _Requirements: P1-6, P1-7_

- [x] 4.2 Implement CovenantEvaluationService with transaction management
  - Implement evaluateCovenants method with @Transactional annotation
  - Calculate appropriate ratio based on covenant type
  - Compare calculated value against threshold using comparison operator
  - Create CovenantResult records with PASS or BREACH status
  - Create BREACH alerts for failed covenants
  - _Requirements: P1-8, P1-9_

- [x] 4.3 Implement TrendAnalysisService
  - Implement detectConsecutiveDeclines method (3 consecutive Current Ratio declines)
  - Implement detectNearThresholdWarnings method (within 5% of threshold)
  - Evaluate declines separately for QUARTERLY and ANNUAL cadence streams
  - Create EARLY_WARNING alerts when patterns detected
  - Use BigDecimal comparison for decline detection
  - _Requirements: P1-10, P1-11_

- [x] 4.4 Implement RiskSummaryService with risk ladder logic
  - Implement getRiskSummary method
  - Count only non-superseded results from latest evaluation cycle
  - Set overallRiskLevel to HIGH if any HIGH severity breach exists
  - Set overallRiskLevel to MEDIUM if any breach or warning exists without HIGH severity breach
  - Set overallRiskLevel to LOW if no breaches or warnings exist
  - Compute results within 500ms
  - _Requirements: P1-14_

- [x] 4.5 Implement LoanService with CRUD operations
  - Implement createLoan method
  - Implement findAllLoans with pagination
  - Implement findLoanById method
  - Implement closeLoan method with status validation
  - _Requirements: P1-1, P1-2, P1-3_

- [x] 4.6 Implement CovenantService
  - Implement createCovenant method
  - Validate loan is ACTIVE before creating covenant
  - Handle unique constraint violation for duplicate covenant types
  - _Requirements: P1-4_

- [x] 4.7 Implement FinancialStatementService with supersession logic
  - Implement submitFinancialStatement method
  - Check for duplicate period statement and mark as superseded
  - Mark related CovenantResults and Alerts as superseded
  - Validate loan is ACTIVE before submission
  - Trigger covenant evaluation after statement creation
  - _Requirements: P1-5, P1-8_

- [x] 4.8 Implement AlertService
  - Implement findAlertsByLoan method with pagination
  - Exclude superseded alerts by default
  - Sort alerts by triggeredTimestampUtc descending
  - _Requirements: P1-13_

### [x] 5. Backend Controller Layer (Phase 1)
- [x] 5.1 Implement LoanController
  - Create POST /api/v1/loans endpoint (createLoan)
  - Create GET /api/v1/loans endpoint with pagination (listLoans)
  - Create GET /api/v1/loans/{id} endpoint (getLoan)
  - Create PATCH /api/v1/loans/{id}/close endpoint (closeLoan)
  - _Requirements: P1-1, P1-2, P1-3_

- [x] 5.2 Implement covenant endpoints in LoanController
  - Create POST /api/v1/loans/{loanId}/covenants endpoint (createCovenant)
  - Create GET /api/v1/loans/{loanId}/covenants endpoint (listCovenants)
  - _Requirements: P1-4_

- [x] 5.3 Implement financial statement endpoints
  - Create POST /api/v1/loans/{loanId}/financial-statements endpoint (submitStatement)
  - Create GET /api/v1/loans/{loanId}/financial-statements endpoint (listStatements)
  - _Requirements: P1-5_

- [x] 5.4 Implement covenant results endpoint
  - Create GET /api/v1/loans/{loanId}/covenant-results endpoint
  - Support pagination and sorting by evaluationTimestampUtc descending
  - Exclude superseded results by default
  - _Requirements: P1-12_

- [x] 5.5 Implement alerts endpoint
  - Create GET /api/v1/loans/{loanId}/alerts endpoint
  - Support pagination and sorting by triggeredTimestampUtc descending
  - Exclude superseded alerts by default
  - _Requirements: P1-13_

- [x] 5.6 Implement risk summary endpoint
  - Create GET /api/v1/loans/{loanId}/risk-summary endpoint
  - Return RiskSummaryResponse with risk metrics
  - _Requirements: P1-14_

- [x] 5.7 Implement SpaForwardController for client-side routing
  - Forward all non-API routes to index.html
  - Exclude /api/*, /actuator/*, /swagger-ui/*, /v3/api-docs/* from forwarding
  - _Requirements: P1-23_

### [x] 6. Error Handling (Phase 1)
- [x] 6.1 Create custom exceptions
  - Create ResourceNotFoundException for HTTP 404 errors
  - Create ConflictException for HTTP 409 errors
  - Create UnprocessableEntityException for HTTP 422 errors
  - _Requirements: P1-15_

- [x] 6.2 Implement GlobalExceptionHandler with RFC7807 support
  - Add @ControllerAdvice annotation
  - Handle ResourceNotFoundException returning ProblemDetail with HTTP 404
  - Handle ConflictException returning ProblemDetail with HTTP 409
  - Handle UnprocessableEntityException returning ProblemDetail with HTTP 422
  - Handle MethodArgumentNotValidException for validation errors
  - Include timestamp and correlation ID in error responses
  - Set Content-Type to application/problem+json
  - _Requirements: P1-15_

### [x] 7. Configuration (Phase 1)
- [x] 7.1 Configure OpenAPI/Swagger
  - Add Springdoc OpenAPI dependency
  - Configure OpenAPI info (title, version, description)
  - Document all endpoints with descriptions and response codes
  - Enable Swagger UI at /swagger-ui.html
  - _Requirements: P1-21_

- [x] 7.2 Configure CORS
  - Allow frontend origin (http://localhost:5173 for development)
  - Allow credentials and common HTTP methods
  - _Requirements: P1-17_

- [x] 7.3 Create DataInitializer for seed data
  - Implement ApplicationRunner to seed demo data on startup
  - Create sample loans with realistic data
  - Create sample covenants for demo loans
  - Create sample financial statements triggering various alert conditions
  - Include examples of passing covenants, breached covenants, consecutive declines, near-threshold warnings
  - Only seed data if database is empty
  - _Requirements: P1-22_

- [x] 7.4 Configure H2 database
  - Set up H2 in-memory database in application.properties
  - Enable H2 console for development
  - Configure JPA to auto-create schema
  - _Requirements: P1-23_

### [x] 8. Frontend Core (Phase 1)
- [x] 8.1 Set up React Router
  - Install react-router-dom
  - Configure routes for login, dashboard, loan detail pages
  - Implement ProtectedRoute component for route guards
  - _Requirements: P1-16, P1-17_

- [x] 8.2 Create AuthContext for mock authentication
  - Implement AuthContext with login/logout functions
  - Store session in localStorage
  - Support demo users: analyst@demo.com, risklead@demo.com
  - Provide isAuthenticated state
  - _Requirements: P1-16_

- [x] 8.3 Create ProtectedRoute component
  - Check authentication status from AuthContext
  - Redirect to login page if not authenticated
  - _Requirements: P1-16_

- [x] 8.4 Create API client with Axios
  - Configure Axios instance with base URL
  - Add request interceptor for authentication headers (Phase 1: no-op)
  - Add response interceptor for error handling
  - _Requirements: P1-17_

- [x] 8.5 Define TypeScript types for API DTOs
  - Create interfaces matching backend DTOs
  - Define types for Loan, Covenant, FinancialStatement, CovenantResult, Alert, RiskSummary
  - _Requirements: P1-17_

### [x] 9. Frontend Pages & Components (Phase 1)
- [x] 9.1 Implement LoginPage
  - Create login form with username and password fields
  - Validate credentials against demo accounts
  - Display error message for invalid credentials
  - Redirect to dashboard on successful login
  - _Requirements: P1-16_

- [x] 9.2 Implement DashboardPage
  - Display welcome message with current user
  - Show navigation to loan list
  - Provide logout button
  - _Requirements: P1-17_

- [x] 9.3 Implement LoanList component
  - Fetch and display paginated loan list
  - Show columns: borrower name, principal amount, start date, status
  - Provide "Create Loan" button opening loan creation form
  - Handle pagination controls
  - _Requirements: P1-17_

- [x] 9.4 Implement LoanDetail component
  - Fetch and display full loan information
  - Show covenants table with type, threshold, operator, severity
  - Show financial statements list
  - Show covenant results table with pagination
  - Show alerts table with pagination
  - Display risk summary panel with color-coded risk level
  - Provide "Close Loan" button for ACTIVE loans
  - _Requirements: P1-17, P1-20_

- [x] 9.5 Implement CovenantForm component
  - Create form with fields: covenant type, threshold value, comparison operator, severity level
  - Provide dropdown selections for enums
  - Validate threshold value is positive number
  - Submit to POST /api/v1/loans/{loanId}/covenants endpoint
  - Display success/error messages
  - _Requirements: P1-18_

- [x] 9.6 Implement StatementForm component
  - Create form with fields: period type, fiscal year, fiscal quarter (conditional), monetary fields
  - Show/hide fiscal quarter based on period type selection
  - Validate all monetary fields are non-negative
  - Submit to POST /api/v1/loans/{loanId}/financial-statements endpoint
  - Display success/error messages
  - _Requirements: P1-19_

- [x] 9.7 Implement AlertList component
  - Display alerts table with columns: type, severity, message, triggered timestamp
  - Support pagination and sorting
  - Color-code severity levels (HIGH: red, MEDIUM: yellow, LOW: green)
  - _Requirements: P1-20_

- [x] 9.8 Implement RiskSummary component
  - Display risk metrics: total covenants, breached covenants, active warnings, overall risk level
  - Color-code overall risk level (HIGH: red, MEDIUM: yellow, LOW: green)
  - _Requirements: P1-20_

- [x] 9.9 Implement TrendChart component with Recharts
  - Install recharts library
  - Display line chart showing ratio values over time
  - Support toggling between different covenant types
  - Show threshold line for comparison
  - _Requirements: P1-20_

### [x] 10. Testing (Phase 1)
- [x] 10.1 Write unit tests for FinancialRatioService
  - Test calculateCurrentRatio with valid inputs
  - Test calculateDebtToEquity with valid inputs
  - Test division by zero handling
  - Test BigDecimal precision and rounding
  - _Requirements: P1-6, P1-7_

- [x] 10.2 Write unit tests for CovenantEvaluationService
  - Test covenant evaluation with PASS status
  - Test covenant evaluation with BREACH status
  - Test BREACH alert creation
  - Test transaction rollback on failure
  - _Requirements: P1-8, P1-9_

- [x] 10.3 Write integration test for full loan flow
  - Test creating loan, adding covenant, submitting statement
  - Verify covenant evaluation triggered automatically
  - Verify alerts created for breaches
  - Verify risk summary calculation
  - _Requirements: P1-1 through P1-14_

- [x] 10.4 Write frontend component tests
  - Test LoginPage with valid/invalid credentials
  - Test LoanList rendering and pagination
  - Test CovenantForm validation and submission
  - Test StatementForm conditional field display
  - _Requirements: P1-16 through P1-20_

### [x] 11. Deployment (Phase 1)
- [x] 11.1 Create Dockerfile with multi-stage build
  - Stage 1: Build frontend with Node.js and Vite
  - Stage 2: Build backend with Maven
  - Stage 3: Copy frontend build to Spring Boot static resources
  - Final stage: Run Spring Boot application with Java 21
  - _Requirements: P1-23_

- [x] 11.2 Create docker-compose.yml
  - Define service for CovenantIQ application
  - Expose port 8080
  - Configure environment variables
  - _Requirements: P1-23_

- [x] 11.3 Configure Spring Boot to serve static assets
  - Configure static resource locations in application.properties
  - Ensure frontend routes handled by SpaForwardController
  - _Requirements: P1-23_

- [x] 11.4 Test Docker container build and run
  - Build Docker image
  - Run container with docker compose up
  - Verify application accessible on http://localhost:8080
  - Test API endpoints and frontend navigation
  - _Requirements: P1-23_

---

## Phase 2 Tasks (PLANNED)

### [ ] 12. Database Schema Extensions (Phase 2)
- [ ] 12.1 Extend FinancialStatement entity with new fields
  - Add fields: netOperatingIncome, totalDebtService, intangibleAssets, ebitda, fixedCharges, inventory, totalAssets, totalLiabilities
  - Use BigDecimal with precision 19, scale 4 for all new monetary fields
  - Add validation annotations for non-negative values
  - Update database schema migration (if using Flyway/Liquibase)
  - _Requirements: 6, 7_

- [ ] 12.2 Extend Alert entity with lifecycle fields
  - Add status field (AlertStatus enum: OPEN, ACKNOWLEDGED, UNDER_REVIEW, RESOLVED)
  - Add acknowledgedBy (User reference), acknowledgedAt (OffsetDateTime)
  - Add resolvedBy (User reference), resolvedAt (OffsetDateTime)
  - Add resolutionNotes (String, max 2000 characters)
  - Set default status to OPEN
  - _Requirements: 2, 3_

- [ ] 12.3 Create User entity
  - Define fields: id, username (unique), passwordHash, email, active, createdAt
  - Add @OneToMany relationship to UserRole
  - _Requirements: 10, 13_

- [ ] 12.4 Create Role entity
  - Define fields: id, name (unique), description
  - Seed roles: ANALYST, RISK_LEAD, ADMIN
  - _Requirements: 12, 13_

- [ ] 12.5 Create UserRole entity
  - Define fields: id, user, role, assignedAt
  - Add @UniqueConstraint on (user_id, role_id)
  - _Requirements: 12, 13_

- [ ] 12.6 Create Comment entity
  - Define fields: id, loan, createdBy (User), commentText (max 5000 chars), createdAt
  - Add @ManyToOne relationship to Loan and User
  - _Requirements: 15_

- [ ] 12.7 Create ActivityLog entity
  - Define fields: id, eventType (ActivityEventType enum), entityType, entityId, user, username, timestamp, description (max 1000 chars)
  - Create ActivityEventType enum with values: LOAN_CREATED, LOAN_UPDATED, COVENANT_CREATED, COVENANT_UPDATED, STATEMENT_SUBMITTED, ALERT_ACKNOWLEDGED, ALERT_RESOLVED, COMMENT_ADDED
  - _Requirements: 16_

- [ ] 12.8 Create Attachment entity
  - Define fields: id, financialStatement, filename, fileSize, contentType, fileData (Lob), uploadedBy (User), uploadedAt
  - Add @ManyToOne relationship to FinancialStatement and User
  - Configure cascade delete when financial statement deleted
  - _Requirements: 14_

- [ ] 12.9 Create AlertStatus enum
  - Define values: OPEN, ACKNOWLEDGED, UNDER_REVIEW, RESOLVED
  - _Requirements: 2_

- [ ] 12.10 Add database indexes for performance
  - Add index on financial_statements(loan_id, fiscal_year, fiscal_quarter)
  - Add index on covenant_results(covenant_id, superseded)
  - Add index on alerts(loan_id, status, superseded)
  - Add index on activity_logs(entity_type, entity_id, timestamp)
  - _Requirements: 9_

- [ ] 12.11 Create repositories for new entities
  - Create UserRepository with findByUsername method
  - Create RoleRepository with findByName method
  - Create UserRoleRepository
  - Create CommentRepository with findByLoanOrderByCreatedAtDesc method
  - Create ActivityLogRepository with queries for loan activity and global activity
  - Create AttachmentRepository with findByFinancialStatement method
  - _Requirements: 10, 13, 14, 15, 16_

### [ ] 13. Authentication & Authorization (Phase 2)
- [ ] 13.1 Add Spring Security dependencies
  - Add spring-boot-starter-security to pom.xml
  - Add jjwt-api, jjwt-impl, jjwt-jackson dependencies for JWT
  - _Requirements: 10_

- [ ] 13.2 Implement JwtTokenProvider utility
  - Implement generateAccessToken method (1 hour expiry)
  - Implement generateRefreshToken method (7 day expiry)
  - Implement validateToken method
  - Implement extractClaims method (userId, username, roles)
  - Use HS256 algorithm with secret key from application.properties
  - Include claims: sub (userId), username, roles, iat, exp
  - _Requirements: 10_

- [ ] 13.3 Implement AuthService
  - Implement authenticate method with username/password validation
  - Use BCrypt.checkPassword to verify password hash
  - Generate access token and refresh token on successful authentication
  - Return AuthResponse with both tokens
  - Throw AuthenticationException for invalid credentials
  - _Requirements: 10_

- [ ] 13.4 Implement password validation and hashing
  - Implement password policy validation (min 8 chars, uppercase, lowercase, digit, special char)
  - Use BCrypt with cost factor 12 for password hashing
  - Return HTTP 400 with specific policy violation message on failure
  - _Requirements: 11_

- [ ] 13.5 Implement account lockout logic
  - Track failed login attempts per username
  - Lock account after 5 consecutive failures within 15 minutes
  - Return HTTP 423 with lockout duration message
  - Reset failed attempts counter on successful login
  - _Requirements: 11_

- [ ] 13.6 Create AuthController
  - Create POST /api/v1/auth/login endpoint (authenticate)
  - Create POST /api/v1/auth/refresh endpoint (refresh access token)
  - Return HTTP 401 for invalid credentials
  - Return HTTP 200 with tokens on success
  - _Requirements: 10_

- [ ] 13.7 Implement JwtAuthenticationFilter
  - Extract JWT from Authorization header (Bearer token)
  - Validate token using JwtTokenProvider
  - Set SecurityContext with authenticated user
  - Return HTTP 401 with WWW-Authenticate header for missing/invalid token
  - _Requirements: 10_

- [ ] 13.8 Implement AuthorizationService with role checks
  - Implement hasRole method checking user roles
  - Implement requireRole method throwing ForbiddenException (HTTP 403) if unauthorized
  - Support multiple roles with combined permissions
  - _Requirements: 12_

- [ ] 13.9 Configure Spring Security filter chain
  - Create SecurityConfig class with @EnableWebSecurity
  - Configure JWT filter in filter chain
  - Permit /api/v1/auth/**, /actuator/health, /swagger-ui/**, /v3/api-docs/** without authentication
  - Require authentication for all other /api/v1/** endpoints
  - Disable CSRF for stateless API
  - _Requirements: 10, 18_

- [ ] 13.10 Add @PreAuthorize annotations to service methods
  - Annotate loan/covenant/statement CRUD methods with @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
  - Annotate alert resolution methods with @PreAuthorize("hasAnyRole('RISK_LEAD', 'ADMIN')")
  - Annotate user management methods with @PreAuthorize("hasRole('ADMIN')")
  - Annotate portfolio summary with @PreAuthorize("hasAnyRole('RISK_LEAD', 'ADMIN')")
  - _Requirements: 12_

- [ ] 13.11 Create seed users with different roles
  - Update DataInitializer to create demo users
  - Create analyst@demo.com with ANALYST role
  - Create risklead@demo.com with RISK_LEAD role
  - Create admin@demo.com with ADMIN role
  - Hash passwords using BCrypt
  - _Requirements: 10, 12, 13_

### [ ] 14. User Management (Phase 2)
- [ ] 14.1 Create UserService with CRUD operations
  - Implement createUser method with password hashing
  - Implement findAllUsers method excluding password fields
  - Implement updateUserRoles method
  - Implement deactivateUser method (set active = false)
  - Prevent deletion of last ADMIN user
  - _Requirements: 13_

- [ ] 14.2 Create UserController
  - Create POST /api/v1/users endpoint (createUser)
  - Create GET /api/v1/users endpoint (listUsers)
  - Create PATCH /api/v1/users/{id} endpoint (updateUser)
  - Create DELETE /api/v1/users/{id} endpoint (deactivateUser)
  - Enforce unique username constraint returning HTTP 409 on duplicate
  - Return HTTP 400 when attempting to delete last ADMIN
  - _Requirements: 13_

- [ ] 14.3 Create user management DTOs
  - Create CreateUserRequest with username, password, email, roles
  - Create UserResponse excluding password field
  - Create UpdateUserRolesRequest
  - _Requirements: 13_

- [ ] 14.4 Add user management UI components
  - Create UserList component displaying all users
  - Create UserForm component for creating users
  - Create UserRoleEditor component for updating roles
  - Add user management page accessible only to ADMIN role
  - _Requirements: 13_

### [ ] 15. Enhanced Covenant Types (Phase 2)
- [ ] 15.1 Update CovenantType enum with 6 new types
  - Add DSCR, INTEREST_COVERAGE, TANGIBLE_NET_WORTH, DEBT_TO_EBITDA, FIXED_CHARGE_COVERAGE, QUICK_RATIO
  - Keep existing CURRENT_RATIO, DEBT_TO_EQUITY
  - _Requirements: 6_

- [ ] 15.2 Implement new ratio calculation methods in FinancialRatioService
  - Implement calculateDSCR (netOperatingIncome / totalDebtService)
  - Implement calculateInterestCoverage (ebit / interestExpense)
  - Implement calculateTangibleNetWorth (totalAssets - intangibleAssets - totalLiabilities)
  - Implement calculateDebtToEBITDA (totalDebt / ebitda)
  - Implement calculateFixedChargeCoverage ((ebit + fixedCharges) / (fixedCharges + interestExpense))
  - Implement calculateQuickRatio ((currentAssets - inventory) / currentLiabilities)
  - Use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places
  - Throw UnprocessableEntityException for division by zero or missing required fields
  - _Requirements: 6_

- [ ] 15.3 Update CovenantEvaluationService to handle new types
  - Update ratio calculation switch statement to include new covenant types
  - Ensure all 8 covenant types properly evaluated
  - _Requirements: 6_

- [ ] 15.4 Update frontend covenant form with new types
  - Add new covenant types to dropdown selection
  - Update CovenantForm component
  - _Requirements: 6_

- [ ] 15.5 Update frontend statement form with new fields
  - Add input fields for: netOperatingIncome, totalDebtService, intangibleAssets, ebitda, fixedCharges, inventory, totalAssets, totalLiabilities
  - Update StatementForm component with validation
  - _Requirements: 7_

### [ ] 16. Alert Lifecycle Management (Phase 2)
- [ ] 16.1 Update AlertService with status transition logic
  - Implement updateAlertStatus method
  - Validate status transitions (prevent RESOLVED -> earlier states)
  - Record acknowledgedBy, acknowledgedAt when status changes to ACKNOWLEDGED
  - Record resolvedBy, resolvedAt, resolutionNotes when status changes to RESOLVED
  - Require resolutionNotes when transitioning to RESOLVED
  - _Requirements: 2, 3_

- [ ] 16.2 Create AlertController endpoint for status updates
  - Create PATCH /api/v1/alerts/{id}/status endpoint
  - Accept status and optional resolutionNotes in request body
  - Return HTTP 400 if resolutionNotes missing for RESOLVED status
  - Return HTTP 400 for invalid status transitions
  - _Requirements: 2, 3_

- [ ] 16.3 Update alert counting logic
  - Exclude RESOLVED alerts from activeWarnings count in RiskSummaryService
  - Update portfolio aggregation to count alerts by status
  - _Requirements: 2, 9_

- [ ] 16.4 Update alert sorting
  - Sort alerts by severity descending, then by triggeredTimestampUtc ascending
  - _Requirements: 2_

- [ ] 16.5 Update AlertList component with status management UI
  - Display alert status column
  - Add "Acknowledge" button for OPEN alerts (ANALYST, RISK_LEAD, ADMIN roles)
  - Add "Resolve" button for ACKNOWLEDGED/UNDER_REVIEW alerts (RISK_LEAD, ADMIN roles)
  - Show resolution notes in alert details
  - _Requirements: 2, 3_

- [ ] 16.6 Create resolution notes modal/form
  - Create modal component for entering resolution notes
  - Validate max length 2000 characters
  - Submit to PATCH /api/v1/alerts/{id}/status endpoint
  - _Requirements: 3_

### [ ] 17. Risk Details Endpoint (Phase 2)
- [ ] 17.1 Create RiskDetailsResponse DTO
  - Define fields: loanId, covenantBreakdowns (list)
  - Define CovenantBreakdown: covenantName, currentValue, thresholdValue, complianceStatus, reasonString, triggeredRuleNames
  - _Requirements: 1_

- [ ] 17.2 Implement getRiskDetails method in RiskSummaryService
  - Fetch latest evaluation cycle results for loan
  - Build covenant-level breakdown with reason strings
  - Include triggered rule names (e.g., "CONSECUTIVE_DECLINE", "NEAR_THRESHOLD")
  - Compute results within 500ms
  - _Requirements: 1_

- [ ] 17.3 Create endpoint GET /api/v1/loans/{id}/risk-details
  - Return HTTP 200 with RiskDetailsResponse
  - Return HTTP 404 for non-existent loan
  - Return HTTP 200 with empty covenant list if no evaluation results
  - _Requirements: 1_

- [ ] 17.4 Add frontend component to display risk details
  - Create RiskDetails component showing covenant breakdown table
  - Display current value, threshold, status, reason for each covenant
  - Color-code compliance status
  - _Requirements: 1_

### [ ] 18. Export Functionality (Phase 2)
- [ ] 18.1 Add OpenCSV dependency
  - Add opencsv dependency to pom.xml
  - _Requirements: 4, 5_

- [ ] 18.2 Implement ExportService
  - Implement generateAlertsCsv method
  - Implement generateCovenantResultsCsv method
  - Format timestamps in ISO 8601 format
  - Format decimal values with 4 decimal places
  - Implement RFC 4180 CSV escaping (commas, quotes)
  - _Requirements: 4, 5_

- [ ] 18.3 Create export endpoints
  - Create GET /api/v1/loans/{loanId}/alerts/export endpoint
  - Create GET /api/v1/loans/{loanId}/covenant-results/export endpoint
  - Set Content-Type to text/csv
  - Set Content-Disposition header with filename
  - Return CSV file with headers even if no data
  - _Requirements: 4, 5_

- [ ] 18.4 Define CSV headers for alerts
  - Headers: Alert ID, Loan ID, Loan Name, Severity, Message, Status, Created At, Acknowledged By, Acknowledged At, Resolved By, Resolved At, Resolution Notes
  - _Requirements: 4_

- [ ] 18.5 Define CSV headers for covenant results
  - Headers: Evaluation Date, Covenant Name, Covenant Type, Calculated Value, Threshold Value, Comparison Operator, Compliance Status, Statement Period End Date
  - Sort results by evaluation date descending, then by covenant name ascending
  - _Requirements: 5_

- [ ] 18.6 Add export buttons to frontend tables
  - Add "Export to CSV" button to AlertList component
  - Add "Export to CSV" button to covenant results table
  - Handle file download in browser
  - _Requirements: 4, 5_

### [ ] 19. Bulk Import (Phase 2)
- [ ] 19.1 Add Apache POI dependency for Excel parsing
  - Add poi-ooxml dependency to pom.xml
  - _Requirements: 8, 22_

- [ ] 19.2 Create BulkImportService
  - Implement parseCsv method using OpenCSV
  - Implement parseExcel method using Apache POI
  - Read data from first worksheet only for Excel
  - Skip empty rows without generating errors
  - _Requirements: 8, 21, 22_

- [ ] 19.3 Implement row-level validation
  - Validate each row independently
  - Continue processing after row failures
  - Collect success/failure results for each row
  - Check for duplicate periodEndDate and mark as failed
  - Parse dates in ISO 8601 format (YYYY-MM-DD)
  - Infer periodType and fiscalQuarter from periodEndDate
  - _Requirements: 8_

- [ ] 19.4 Create BulkImportSummary DTO
  - Define fields: totalRows, successCount, failureCount, rowResults
  - Define RowResult: rowNumber, success, errorMessage
  - _Requirements: 8_

- [ ] 19.5 Create bulk import endpoint
  - Create POST /api/v1/loans/{loanId}/financial-statements/bulk-import endpoint
  - Accept CSV and Excel (.xlsx) files
  - Reject files > 5MB with HTTP 413
  - Return HTTP 200 with BulkImportSummary
  - _Requirements: 8_

- [ ] 19.6 Define CSV/Excel column structure
  - Columns: periodEndDate, totalAssets, totalLiabilities, currentAssets, currentLiabilities, totalDebt, totalEquity, netOperatingIncome, totalDebtService, ebit, interestExpense, intangibleAssets, ebitda, fixedCharges, inventory
  - _Requirements: 8_

- [ ] 19.7 Add file upload UI component
  - Create BulkImportForm component with file input
  - Support CSV and Excel file types
  - Display upload progress
  - Show import summary with success/failure breakdown
  - Display row-level errors in table
  - _Requirements: 8_

### [ ] 20. Portfolio Aggregation (Phase 2)
- [ ] 20.1 Create PortfolioSummaryResponse DTO
  - Define fields: totalActiveLoans, totalBreaches, highRiskLoanCount, mediumRiskLoanCount, lowRiskLoanCount, totalOpenAlerts, totalUnderReviewAlerts
  - _Requirements: 9_

- [ ] 20.2 Implement PortfolioAggregator service
  - Implement getPortfolioSummary method
  - Count only Active_Loan records (status != CLOSED and status != PAID_OFF)
  - Define highRiskLoanCount as loans with >= 2 covenant breaches in latest cycle
  - Define mediumRiskLoanCount as loans with exactly 1 covenant breach in latest cycle
  - Define lowRiskLoanCount as active loans with 0 breaches in latest cycle
  - Count alerts with status OPEN in totalOpenAlerts
  - Count alerts with status UNDER_REVIEW in totalUnderReviewAlerts
  - _Requirements: 9_

- [ ] 20.3 Optimize query performance for large portfolios
  - Use efficient JPQL queries with joins
  - Avoid N+1 query problems
  - Test with 1000+ active loans
  - Ensure computation completes within 2 seconds
  - _Requirements: 9_

- [ ] 20.4 Create endpoint GET /api/v1/portfolio/summary
  - Return HTTP 200 with PortfolioSummaryResponse
  - Require RISK_LEAD or ADMIN role
  - _Requirements: 9_

- [ ] 20.5 Create PortfolioPage component
  - Display portfolio-wide metrics in cards
  - Show distribution of loans by risk level (pie chart)
  - Show alert status breakdown (bar chart)
  - Display top 10 high-risk loans table
  - _Requirements: 9_

### [ ] 21. Document Attachments (Phase 2)
- [ ] 21.1 Implement DocumentStore service
  - Implement storeAttachment method
  - Validate PDF file type (Content-Type: application/pdf)
  - Validate file size (max 10MB)
  - Store file as byte[] in Attachment.fileData
  - Return HTTP 415 for non-PDF files
  - Return HTTP 413 for files > 10MB
  - _Requirements: 14_

- [ ] 21.2 Create attachment endpoints
  - Create POST /api/v1/financial-statements/{id}/attachments endpoint (upload)
  - Create GET /api/v1/financial-statements/{id}/attachments endpoint (list)
  - Create GET /api/v1/financial-statements/{id}/attachments/{attachmentId} endpoint (download)
  - Create DELETE /api/v1/financial-statements/{id}/attachments/{attachmentId} endpoint (delete)
  - Set Content-Type to application/pdf for download
  - _Requirements: 14_

- [ ] 21.3 Implement cascade delete
  - Configure @OneToMany relationship with cascade = CascadeType.ALL
  - Verify attachments deleted when financial statement deleted
  - _Requirements: 14_

- [ ] 21.4 Create attachment DTOs
  - Create AttachmentResponse with metadata (id, filename, fileSize, contentType, uploadedBy, uploadedAt)
  - Exclude fileData from list responses
  - _Requirements: 14_

- [ ] 21.5 Add file upload component to statement form
  - Add file input to StatementForm component
  - Support PDF file selection
  - Display file size validation
  - Show upload progress
  - _Requirements: 14_

- [ ] 21.6 Add attachment list/download UI
  - Create AttachmentList component
  - Display filename, size, uploaded by, uploaded date
  - Provide download button for each attachment
  - Provide delete button (with confirmation)
  - _Requirements: 14_

### [ ] 22. Collaboration Features (Phase 2)
- [ ] 22.1 Implement CommentService
  - Implement createComment method
  - Implement findCommentsByLoan method (sorted by createdAt descending)
  - Implement deleteComment method with permission check (creator or ADMIN only)
  - Enforce max comment length 5000 characters
  - _Requirements: 15_

- [ ] 22.2 Create comment endpoints
  - Create POST /api/v1/loans/{loanId}/comments endpoint (createComment)
  - Create GET /api/v1/loans/{loanId}/comments endpoint (listComments)
  - Create DELETE /api/v1/loans/{loanId}/comments/{commentId} endpoint (deleteComment)
  - Return HTTP 403 if user not authorized to delete comment
  - _Requirements: 15_

- [ ] 22.3 Create comment DTOs
  - Create CreateCommentRequest with commentText field
  - Create CommentResponse with id, commentText, createdBy (username), createdAt
  - _Requirements: 15_

- [ ] 22.4 Create CommentSection component
  - Display comments list with author and timestamp
  - Show comment form for adding new comments
  - Provide delete button for own comments (or ADMIN)
  - Auto-refresh after adding/deleting comment
  - _Requirements: 15_

- [ ] 22.5 Add comment section to loan detail page
  - Integrate CommentSection component into LoanDetail page
  - Position below risk summary and above activity log
  - _Requirements: 15_

### [ ] 23. Activity Logging (Phase 2)
- [ ] 23.1 Implement ActivityLogger service
  - Implement logActivity method with @Async annotation
  - Generate human-readable descriptions (e.g., "John Doe acknowledged alert #123")
  - Store activity metadata: eventType, entityType, entityId, user, username, timestamp, description
  - _Requirements: 16_

- [ ] 23.2 Add activity logging to business operations
  - Log LOAN_CREATED when loan created
  - Log LOAN_UPDATED when loan updated
  - Log COVENANT_CREATED when covenant created
  - Log COVENANT_UPDATED when covenant updated
  - Log STATEMENT_SUBMITTED when financial statement submitted
  - Log ALERT_ACKNOWLEDGED when alert acknowledged
  - Log ALERT_RESOLVED when alert resolved
  - Log COMMENT_ADDED when comment added
  - _Requirements: 16_

- [ ] 23.3 Create activity endpoints
  - Create GET /api/v1/loans/{loanId}/activity endpoint (loan-specific activity)
  - Create GET /api/v1/activity endpoint with date range parameters (global activity)
  - Sort by timestamp descending
  - Support pagination
  - _Requirements: 16_

- [ ] 23.4 Implement activity log retention
  - Configure scheduled task to delete logs older than 90 days
  - Run cleanup daily at midnight
  - _Requirements: 16_

- [ ] 23.5 Create ActivityLog component
  - Display activity timeline with event type, description, user, timestamp
  - Use icons for different event types
  - Show relative timestamps (e.g., "2 hours ago")
  - _Requirements: 16_

- [ ] 23.6 Add activity timeline to loan detail page
  - Integrate ActivityLog component into LoanDetail page
  - Position at bottom of page
  - Load activities on page load
  - _Requirements: 16_

### [ ] 24. Enhanced Early Warning Detection (Phase 2)
- [ ] 24.1 Implement volatility calculation in TrendAnalysisService
  - Implement calculateVolatility method (standard deviation over last 4 periods)
  - Use BigDecimal arithmetic with HALF_UP rounding to 4 decimal places
  - Require minimum 4 historical periods before calculating
  - Skip volatility detection without error if insufficient data
  - _Requirements: 19_

- [ ] 24.2 Implement volatility detection
  - Check if ratio volatility exceeds 0.3 for any covenant
  - Create EARLY_WARNING alert with severity MEDIUM when detected
  - Set message to indicate high volatility with specific covenant details
  - _Requirements: 19_

- [ ] 24.3 Implement seasonal anomaly detection in TrendAnalysisService
  - Implement detectSeasonalAnomaly method
  - Compare current ratio to same quarter in previous year
  - Calculate percentage deviation: |((current - historical) / historical)| * 100
  - Require minimum 4 quarters of historical data
  - Skip seasonal anomaly detection without error if insufficient data
  - _Requirements: 20_

- [ ] 24.4 Create alerts for seasonal anomalies
  - Create EARLY_WARNING alert with severity LOW when deviation > 25%
  - Set message to indicate seasonal anomaly with specific details
  - _Requirements: 20_

- [ ] 24.5 Integrate new detection rules into statement submission flow
  - Call volatility detection after covenant evaluation
  - Call seasonal anomaly detection after covenant evaluation
  - Ensure alerts created in same transaction
  - _Requirements: 19, 20_

- [ ] 24.6 Update frontend to display new alert types
  - Update AlertList component to show volatility and seasonal anomaly alerts
  - Add tooltips explaining alert types
  - _Requirements: 19, 20_

### [ ] 25. Structured Logging (Phase 2)
- [ ] 25.1 Add Logback JSON encoder dependency
  - Add logstash-logback-encoder dependency to pom.xml
  - _Requirements: 17_

- [ ] 25.2 Configure JSON log format in logback-spring.xml
  - Configure JsonEncoder with fields: timestamp, level, logger, message, correlationId, userId, requestPath, responseStatus
  - Configure console appender with JSON format
  - Configure file appender with JSON format and rotation (max 10MB per file, keep 30 days)
  - _Requirements: 17_

- [ ] 25.3 Implement CorrelationIdFilter
  - Generate unique correlation ID for each incoming HTTP request
  - Store correlation ID in MDC (Mapped Diagnostic Context)
  - Add X-Correlation-ID to response headers
  - Clear MDC after request completion
  - _Requirements: 17_

- [ ] 25.4 Add MDC context for userId, requestPath, responseStatus
  - Extract userId from SecurityContext and add to MDC
  - Extract requestPath from HttpServletRequest and add to MDC
  - Extract responseStatus from HttpServletResponse and add to MDC
  - _Requirements: 17_

- [ ] 25.5 Add structured logging to business events
  - Log covenant breach detected at INFO level
  - Log alert created at INFO level
  - Log alert resolved at INFO level
  - Log bulk import completed at INFO level with row counts
  - Log errors at ERROR level with stack traces
  - _Requirements: 17_

- [ ] 25.6 Log authentication failures
  - Log authentication failures at WARN level
  - Include username and IP address (without sensitive data)
  - _Requirements: 17_

### [ ] 26. Health Check (Phase 2)
- [ ] 26.1 Enable Spring Boot Actuator
  - Add spring-boot-starter-actuator dependency to pom.xml
  - Configure actuator endpoints in application.properties
  - _Requirements: 18_

- [ ] 26.2 Configure /actuator/health endpoint
  - Enable health endpoint
  - Show component details (database, disk space)
  - _Requirements: 18_

- [ ] 26.3 Add database health indicator
  - Use default DataSourceHealthIndicator
  - Return status DOWN when database connection fails
  - _Requirements: 18_

- [ ] 26.4 Add disk space health indicator
  - Use default DiskSpaceHealthIndicator
  - Configure threshold for low disk space warning
  - _Requirements: 18_

- [ ] 26.5 Exclude health endpoint from authentication
  - Update SecurityConfig to permit /actuator/health without authentication
  - Ensure health check responds within 1 second
  - _Requirements: 18_

- [ ] 26.6 Test health check endpoint
  - Test GET /actuator/health returns HTTP 200 with status UP when healthy
  - Test returns HTTP 503 with status DOWN when database unavailable
  - Verify response time < 1 second
  - _Requirements: 18_

### [ ] 27. Testing (Phase 2)
- [ ]* 27.1 Write unit tests for new FinancialRatioService methods
  - Test calculateDSCR with valid inputs
  - Test calculateInterestCoverage with valid inputs
  - Test calculateTangibleNetWorth with valid inputs
  - Test calculateDebtToEBITDA with valid inputs
  - Test calculateFixedChargeCoverage with valid inputs
  - Test calculateQuickRatio with valid inputs
  - Test division by zero handling for all new methods
  - Test missing required fields handling
  - _Requirements: 6_

- [ ]* 27.2 Write unit tests for AuthService
  - Test successful authentication with valid credentials
  - Test authentication failure with invalid credentials
  - Test JWT token generation with correct claims
  - Test password hashing with BCrypt
  - Test password policy validation
  - Test account lockout after 5 failed attempts
  - _Requirements: 10, 11_

- [ ]* 27.3 Write unit tests for ExportService
  - Test CSV generation for alerts with all fields
  - Test CSV generation for covenant results with sorting
  - Test RFC 4180 escaping (commas, quotes)
  - Test timestamp formatting in ISO 8601
  - Test decimal formatting with 4 decimal places
  - _Requirements: 4, 5_

- [ ]* 27.4 Write unit tests for BulkImportService
  - Test CSV parsing with valid data
  - Test Excel parsing with valid data
  - Test row-level validation and error collection
  - Test duplicate periodEndDate detection
  - Test file size validation (reject > 5MB)
  - Test empty row handling
  - _Requirements: 8, 21, 22_

- [ ]* 27.5 Write unit tests for PortfolioAggregator
  - Test portfolio metrics calculation with various loan states
  - Test risk level counting (high/medium/low)
  - Test alert status counting
  - Test with 1000+ active loans for performance
  - _Requirements: 9_

- [ ]* 27.6 Write integration test for alert lifecycle
  - Test alert creation with OPEN status
  - Test status transition to ACKNOWLEDGED
  - Test status transition to UNDER_REVIEW
  - Test status transition to RESOLVED with resolution notes
  - Test invalid status transitions (RESOLVED -> earlier states)
  - Test resolution notes requirement for RESOLVED status
  - _Requirements: 2, 3_

- [ ]* 27.7 Write integration test for bulk import
  - Test end-to-end CSV upload and processing
  - Test end-to-end Excel upload and processing
  - Test import summary with success/failure counts
  - Test row-level error reporting
  - _Requirements: 8_

- [ ]* 27.8 Write integration test for authentication flow
  - Test login with valid credentials receiving JWT tokens
  - Test protected endpoint access with valid JWT
  - Test protected endpoint access without JWT (HTTP 401)
  - Test protected endpoint access with expired JWT (HTTP 401)
  - Test token refresh with valid refresh token
  - Test role-based access control (HTTP 403 for unauthorized roles)
  - _Requirements: 10, 12_

- [ ]* 27.9 Write property-based tests for all 26 properties
  - Configure jqwik with 100+ iterations per property
  - Write property tests for financial ratio calculations
  - Write property tests for covenant evaluation logic
  - Write property tests for CSV parsing/formatting round-trip
  - Write property tests for alert lifecycle state transitions
  - Write property tests for role-based authorization
  - _Requirements: All Phase 2 requirements_

- [ ]* 27.10 Measure code coverage
  - Run test suite with coverage tool (JaCoCo)
  - Verify 80%+ code coverage for Phase 2 code
  - Identify untested code paths
  - _Requirements: All Phase 2 requirements_

### [ ] 28. Frontend Authentication Integration (Phase 2)
- [ ] 28.1 Update AuthContext with JWT logic
  - Replace mock authentication with real API calls
  - Store access token and refresh token in localStorage
  - Implement token refresh logic
  - Provide user roles from JWT claims
  - _Requirements: 10_

- [ ] 28.2 Implement login with real API call
  - Call POST /api/v1/auth/login with username/password
  - Store tokens in localStorage on success
  - Extract user info from JWT claims
  - Display error message on failure
  - _Requirements: 10_

- [ ] 28.3 Add JWT to request headers
  - Update Axios interceptor to add Authorization: Bearer {accessToken} header
  - Include token in all API requests
  - _Requirements: 10_

- [ ] 28.4 Implement token refresh logic
  - Detect 401 responses from API
  - Attempt token refresh using refresh token
  - Retry original request with new access token
  - Redirect to login if refresh fails
  - _Requirements: 10_

- [ ] 28.5 Handle 401 responses with token refresh
  - Add response interceptor to detect 401 errors
  - Implement automatic token refresh and request retry
  - Clear tokens and redirect to login after refresh failure
  - _Requirements: 10_

- [ ] 28.6 Add logout functionality
  - Clear tokens from localStorage
  - Clear user state from AuthContext
  - Redirect to login page
  - _Requirements: 10_

### [ ] 29. Frontend Role-Based UI (Phase 2)
- [ ] 29.1 Add role checks to AuthContext
  - Implement hasRole method checking user roles from JWT
  - Implement hasAnyRole method for multiple role checks
  - Expose role checking functions to components
  - _Requirements: 12_

- [ ] 29.2 Hide/show UI elements based on roles
  - Hide "Create Loan" button for RISK_LEAD role
  - Hide "Add Covenant" button for RISK_LEAD role
  - Hide "Submit Statement" button for RISK_LEAD role
  - Show "Resolve Alert" button only for RISK_LEAD and ADMIN roles
  - Show "User Management" menu only for ADMIN role
  - Show "Portfolio Summary" menu only for RISK_LEAD and ADMIN roles
  - _Requirements: 12_

- [ ] 29.3 Disable actions not permitted by role
  - Disable form submit buttons for unauthorized actions
  - Show tooltip explaining permission requirement
  - _Requirements: 12_

- [ ] 29.4 Show appropriate error messages for forbidden actions
  - Display user-friendly message when API returns HTTP 403
  - Explain which role is required for the action
  - _Requirements: 12_

### [ ] 30. Documentation Updates (Phase 2)
- [ ] 30.1 Update README with Phase 2 features
  - Document new authentication system
  - Document user roles and permissions
  - Document new covenant types
  - Document bulk import functionality
  - Document export functionality
  - Document collaboration features
  - _Requirements: All Phase 2 requirements_

- [ ] 30.2 Update API documentation in Swagger
  - Document all new endpoints with descriptions
  - Document authentication requirements
  - Document role-based access control
  - Document request/response schemas for new DTOs
  - _Requirements: All Phase 2 requirements_

- [ ] 30.3 Document new environment variables
  - Document JWT_SECRET for token signing
  - Document JWT_ACCESS_TOKEN_EXPIRY
  - Document JWT_REFRESH_TOKEN_EXPIRY
  - Document file upload limits
  - _Requirements: 10, 14_

- [ ] 30.4 Update deployment instructions
  - Document database migration steps (if using Flyway/Liquibase)
  - Document seed user creation
  - Document health check endpoint usage
  - _Requirements: 18_

- [ ] 30.5 Document user roles and permissions
  - Create permissions matrix table
  - Document role assignment process
  - Document default admin account setup
  - _Requirements: 12, 13_

### [ ] 31. Performance Optimization (Phase 2)
- [ ] 31.1 Add database indexes for common queries
  - Add index on financial_statements(loan_id, fiscal_year, fiscal_quarter)
  - Add index on covenant_results(covenant_id, superseded)
  - Add index on alerts(loan_id, status, superseded)
  - Add index on activity_logs(entity_type, entity_id, timestamp)
  - Add index on users(username)
  - _Requirements: 9_

- [ ] 31.2 Optimize portfolio aggregation query
  - Use efficient JPQL with joins to minimize database round-trips
  - Test query execution plan
  - Verify N+1 query problems eliminated
  - _Requirements: 9_

- [ ] 31.3 Add caching for frequently accessed data (optional)
  - Consider caching user roles
  - Consider caching loan risk summaries (with TTL)
  - Use Spring Cache abstraction
  - _Requirements: 9_

- [ ] 31.4 Test performance targets
  - Verify risk summary endpoint responds < 500ms
  - Verify portfolio summary endpoint responds < 2s for 1000 loans
  - Verify health check endpoint responds < 1s
  - Load test with realistic data volumes
  - _Requirements: 1, 9, 18_

### [ ] 32. Security Hardening (Phase 2)
- [ ] 32.1 Review and test JWT token expiration
  - Verify access tokens expire after 1 hour
  - Verify refresh tokens expire after 7 days
  - Test token refresh flow
  - _Requirements: 10_

- [ ] 32.2 Test account lockout mechanism
  - Verify account locks after 5 failed login attempts
  - Verify lockout duration (15 minutes)
  - Verify lockout reset on successful login
  - _Requirements: 11_

- [ ] 32.3 Validate password policy enforcement
  - Test minimum 8 characters requirement
  - Test uppercase/lowercase/digit/special char requirements
  - Test password policy error messages
  - _Requirements: 11_

- [ ] 32.4 Test role-based access control on all endpoints
  - Test ANALYST can create/update/delete loans, covenants, statements
  - Test RISK_LEAD cannot create/update/delete loans, covenants, statements
  - Test RISK_LEAD can resolve alerts
  - Test ANALYST cannot resolve alerts
  - Test ADMIN can manage users
  - Test ANALYST and RISK_LEAD cannot manage users
  - Verify HTTP 403 returned for unauthorized actions
  - _Requirements: 12_

- [ ] 32.5 Perform security audit of authentication flow
  - Review JWT secret key strength
  - Review password hashing configuration (BCrypt cost factor)
  - Review CORS configuration
  - Review error messages for information leakage
  - Test for common vulnerabilities (SQL injection, XSS)
  - _Requirements: 10, 11_

### [ ] 33. Final Integration & Testing (Phase 2)
- [ ] 33.1 Run full test suite
  - Run all unit tests (Phase 1 + Phase 2)
  - Run all integration tests (Phase 1 + Phase 2)
  - Run all property-based tests
  - Verify all tests pass
  - _Requirements: All Phase 2 requirements_

- [ ] 33.2 Test Docker build with Phase 2 changes
  - Build Docker image with updated dependencies
  - Verify frontend build includes new components
  - Verify backend includes all new features
  - Test container startup and health check
  - _Requirements: All Phase 2 requirements_

- [ ] 33.3 Perform end-to-end testing of all workflows
  - Test user registration and login flow
  - Test loan creation with all 8 covenant types
  - Test financial statement submission with all fields
  - Test covenant evaluation for all covenant types
  - Test alert lifecycle (OPEN -> ACKNOWLEDGED -> RESOLVED)
  - Test bulk import with CSV and Excel files
  - Test document attachment upload and download
  - Test comment creation and deletion
  - Test activity log recording
  - Test portfolio summary calculation
  - Test CSV export for alerts and covenant results
  - Test role-based access control across all features
  - _Requirements: All Phase 2 requirements_

- [ ] 33.4 Test with realistic data volumes
  - Create 100+ loans with covenants
  - Submit 1000+ financial statements
  - Generate 500+ alerts
  - Test portfolio aggregation performance
  - Test pagination and sorting on large datasets
  - _Requirements: 9_

- [ ] 33.5 Verify all acceptance criteria met
  - Review requirements document
  - Verify each acceptance criterion satisfied
  - Document any deviations or limitations
  - _Requirements: All Phase 2 requirements_

- [ ] 33.6 Fix any bugs discovered during testing
  - Prioritize critical bugs blocking core functionality
  - Fix high-priority bugs affecting user experience
  - Document known issues and workarounds
  - _Requirements: All Phase 2 requirements_

---

## Notes

- Tasks marked with `*` are optional testing tasks and can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Phase 1 tasks (marked [x]) are completed and serve as foundation for Phase 2
- Phase 2 tasks (marked [ ]) are planned for implementation
- All monetary calculations use BigDecimal with HALF_UP rounding to 4 decimal places
- All timestamps stored in UTC using OffsetDateTime
- Authentication uses JWT with 1-hour access tokens and 7-day refresh tokens
- Role-based access control enforced at service layer with @PreAuthorize annotations
- Database uses H2 in-memory for demo; design supports migration to PostgreSQL/MySQL
- Single Docker container deployment serves both API and frontend SPA
