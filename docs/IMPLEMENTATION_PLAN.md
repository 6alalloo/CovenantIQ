# CovenantIQ Implementation Plan

## 1. Delivery Goal

Ship a production-style hackathon MVP in one sprint:
- Spring Boot backend + React frontend
- Full analyst flow operational
- One Docker image/container for Dockploy deployment

## 2. Workstreams

### A. Foundation
- [x] Initialize backend project skeleton (Java 21, Spring Boot, Maven)
- [x] Initialize frontend (React + TS + Vite + Tailwind)
- [x] Establish package/module structure and coding conventions

### B. Backend Domain and APIs
- [x] Create enums, entities, repositories, and database constraints
- [x] Implement DTOs + bean validation
- [x] Implement exception handling with RFC7807 problem details
- [x] Implement loan/covenant/statement/results/alerts/risk-summary endpoints
- [x] Implement close-loan endpoint and state checks

### C. Core Business Logic
- [x] Financial ratio calculations
- [x] Covenant evaluation and breach alert generation
- [x] Trend analysis rules (decline + near-threshold)
- [x] Soft-supersession handling for statement upserts
- [x] Risk summary aggregation and risk ladder

### D. Frontend UX
- [x] Mock login/session and route guards
- [x] Loan list/detail views
- [x] Covenant creation and statement submission forms
- [x] Results and alerts tables with pagination controls
- [x] Risk summary panel and Recharts trend chart(s)
- [x] Error-state and validation rendering from RFC7807 payloads

### E. Documentation and Operations
- [x] OpenAPI/Swagger integration
- [x] Seed data initializer for demo scenarios
- [x] Single-container Docker build with SPA served by Spring
- [x] Runbook updates and demo script

## 2.1 Current Execution Status
- [x] Backend unit tests passing (`FinancialRatioServiceTest`, `CovenantEvaluationServiceTest`)
- [x] Backend integration test passing (`LoanFlowIntegrationTest`)
- [x] Backend package build passing (`mvn -q -DskipTests package`)
- [x] Frontend production build passing (`npm run build`)
- [ ] Docker image build validated locally (blocked: local Docker daemon not reachable in this session)

## 3. Sprint Timeline (1 Week)

### Day 1
- Repo scaffolding (backend + frontend)
- Build tooling and baseline configs
- Domain enums/entities/repositories

### Day 2
- DTO contracts and validation rules
- Loan + covenant APIs
- Error handling framework

### Day 3
- Statement ingestion API
- Ratio and covenant evaluation logic
- Result and breach alert persistence

### Day 4
- Trend analysis rules
- Risk summary endpoint
- Supersession logic and active-cycle filtering

### Day 5
- Frontend core screens (loan, covenant, statement, risk)
- API client integration and mock auth
- Chart and table integration

### Day 6
- Unit + integration testing hardening
- Seed data and OpenAPI polish
- Docker single-container flow end-to-end

### Day 7
- Bug fixing and refactor pass
- Acceptance checklist validation
- Demo rehearsal and handoff packaging

## 4. Definition of Done by Track

### Backend Done
- All required endpoints implemented under `/api/v1`
- Business logic only in services
- RFC7807 errors consistent across failures
- H2 runtime working and seed data available

### Frontend Done
- Core analyst user journeys complete
- UI reads/writes via backend APIs
- Trend chart and risk summary visible per loan
- Error and loading states handled

### Deployment Done
- Docker image builds successfully
- Single container serves API + UI
- Launch instructions reproducible on a clean machine

## 5. Acceptance Test Scenarios

1. Create loan -> add covenant -> submit statement -> see results and alerts.
2. Submit additional statements to trigger trend decline warning.
3. Trigger near-threshold warning and verify alert creation.
4. Trigger breach and verify risk level escalation.
5. Close loan and confirm new submissions are blocked with 409.
6. Resubmit same period and verify prior version is superseded.
7. Validate UI displays paginated tables and trend chart from API data.

## 6. Risks and Mitigations

- **Risk:** Frontend-backend schema mismatch  
  **Mitigation:** Typed DTOs and early API contract freeze.
- **Risk:** One-container SPA routing issues  
  **Mitigation:** Configure Spring static resource fallback for client routes.
- **Risk:** Timeline squeeze from full-stack scope  
  **Mitigation:** Protect core analyst flows first; defer polish-only items.

## 7. Final Locked Defaults

- Java 21 LTS target
- UTC canonical timestamps
- Atomic statement processing transaction
- Synchronous evaluation
- One covenant type per loan
- Warning count per triggered condition
- Frontend mock auth only
- Recharts for trend visualization
- Single-container deployment with Spring serving SPA assets
