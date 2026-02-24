# CovenantIQ Phase 2A Progress Report

## Report Snapshot
- Date: `2026-02-24`
- Scope reference: `docs/PHASE_2A_EXECUTION_CUT.md`
- Status: **Partially implemented (backend-heavy pass complete, validation pending)**

## Summary
Phase 2A execution has started and core backend scaffolding is now in place for authentication, role enforcement, alert lifecycle, risk-details, portfolio summary, exports, and correlation logging. Frontend integration and post-change verification are still pending.

## Completed So Far
1. Added backend dependencies for Spring Security, JWT, Actuator, and structured logging encoder.
2. Added security configuration with JWT filter, stateless session mode, role-based endpoint protection, and public health/auth/docs routes.
3. Added correlation ID filter and response header propagation (`X-Correlation-ID`).
4. Added user auth domain and repository (`UserAccount`, `UserAccountRepository`).
5. Added auth flow:
   - `POST /api/v1/auth/login`
   - `POST /api/v1/auth/refresh`
6. Added alert lifecycle model and transition API:
   - `AlertStatus` enum and lifecycle fields on `Alert`
   - `PATCH /api/v1/alerts/{id}/status`
7. Added risk explainability endpoint:
   - `GET /api/v1/loans/{loanId}/risk-details`
8. Added portfolio aggregation endpoint:
   - `GET /api/v1/portfolio/summary`
9. Added export endpoints:
   - `GET /api/v1/loans/{loanId}/alerts/export`
   - `GET /api/v1/loans/{loanId}/covenant-results/export`
10. Added actuator health configuration and security allowance for `/actuator/health`.
11. Updated seed initializer to create demo users for auth.
12. Updated test property in integration test to allow test execution path with security disabled.

## In Progress / Not Done Yet
1. Frontend Phase 2A integration is **not done**:
   - JWT login/refresh flow
   - Role-based UI gating
   - Alert lifecycle actions in UI
   - Risk details view
   - Portfolio summary view
   - CSV export controls
2. Backend verification is **not done**:
   - `mvn test`
   - `mvn -DskipTests package`
   - manual API smoke checks for new endpoints
3. Frontend verification is **not done**:
   - `npm run build`
   - manual SPA flow checks
4. Docker smoke validation is **not done** after current changes.

## Risks / Notes
1. Multiple backend changes were introduced without a compile/test pass yet, so there is unresolved integration risk until build/test execution.
2. The frontend still uses the previous mock-auth flow at this snapshot; end-to-end auth/RBAC behavior is not complete.
3. Existing untracked project docs/spec files remain in the working tree and were not modified as part of this status update.

## Files Touched in This Phase 2A Pass (high-level)
1. `pom.xml`
2. `src/main/resources/application.yml`
3. `src/main/resources/logback-spring.xml`
4. Security/auth config, filters, DTOs, services, controllers
5. Alert domain/DTO/repository/service updates
6. Loan controller updates for role gating + new endpoints
7. Risk summary service extensions and portfolio/export services
8. Seed data updates and integration-test property update

## Recommended Next Step
1. Run backend compile/tests immediately and fix any breakages before continuing frontend work.
2. Implement frontend Phase 2A integration once backend is stable.
3. Run full Phase 2A acceptance verification and update this report from "partial" to "complete".
