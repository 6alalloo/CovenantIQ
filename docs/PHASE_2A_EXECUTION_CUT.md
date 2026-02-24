# CovenantIQ Phase 2A Execution Cut

## Summary
This document defines an execution-ready Phase 2A scope, explicit defaults, and acceptance checks, with lower-confidence items deferred to Phase 2B.

## Phase 2A Scope (Do Now)
1. Security baseline: JWT auth + refresh + RBAC (`ANALYST`, `RISK_LEAD`, `ADMIN`).
2. Alert lifecycle: `OPEN -> ACKNOWLEDGED -> UNDER_REVIEW -> RESOLVED` with transition guards.
3. Risk explainability: `GET /api/v1/loans/{id}/risk-details`.
4. Portfolio summary: `GET /api/v1/portfolio/summary`.
5. CSV exports: alerts + covenant results.
6. Structured logging + correlation IDs.
7. Health endpoint hardening (`/actuator/health` public).

## Phase 2B Scope (Defer)
1. Bulk CSV/Excel import.
2. Attachments (PDF blob storage).
3. Comments + activity timeline UI.
4. Volatility and seasonality warning rules.
5. Property-based testing rollout.

## Locked Defaults (Decision Complete)
1. Loan status model remains `ACTIVE/CLOSED` in Phase 2A (no `PAID_OFF`).
2. No covenant active/inactive flag in Phase 2A.
3. Monetary precision contract: input accepts up to 4 decimals, storage/calculation at 4 decimals.
4. Error semantics:
   - `400`: malformed payload / schema parse errors
   - `401/403`: authentication / authorization errors
   - `409`: invalid lifecycle or state transition
   - `422`: business/domain validation failures
5. Testing for touched Phase 2A backend/frontend paths is mandatory (not optional).

## Implementation Order
1. Add security foundation: entities/repositories/config/filter/token provider/auth endpoints.
2. Apply role enforcement on existing and new endpoints.
3. Extend `Alert` domain/service/controller for lifecycle transitions.
4. Implement `risk-details` and `portfolio/summary` services/endpoints.
5. Implement CSV export service/endpoints.
6. Add logging/correlation filter and actuator health configuration.
7. Frontend integration: replace mock auth, wire role gates, add alert lifecycle actions, risk details, portfolio page, export buttons.
8. Execute test pass: unit and integration for all new APIs and critical transitions.
9. Run Docker smoke check and update runbook.

## Phase 2A Acceptance Checks
1. Login and refresh succeed; protected routes reject invalid/expired JWT.
2. RBAC returns `403` for forbidden actions.
3. Alert lifecycle enforces legal transitions and requires resolution notes when resolving.
4. Risk details and portfolio summary return expected aggregates with stable contracts.
5. CSV exports download correctly with headers and escaping.
6. Logs include `correlationId`; API responses include `X-Correlation-ID`.
7. `/actuator/health` is unauthenticated and reports component health.

## Current Implementation Status (as of 2026-02-24)
1. Security baseline: **In Progress (backend implemented, frontend not yet wired, not validated)**.
2. Alert lifecycle: **In Progress (domain/service/controller implemented, not validated)**.
3. Risk details endpoint: **In Progress (service + endpoint implemented, not validated)**.
4. Portfolio summary endpoint: **In Progress (service + endpoint implemented, not validated)**.
5. CSV exports: **In Progress (service + endpoints implemented, not validated)**.
6. Structured logging + correlation IDs: **In Progress (filter + logback config implemented, not validated)**.
7. Health endpoint hardening: **In Progress (actuator dependency/config + security rule implemented, not validated)**.

## Current Phase 2A Position
1. Backend foundation for all 2A items has been added.
2. Frontend integration for JWT/RBAC/alert actions/portfolio/risk-details/export is still pending.
3. End-to-end validation (unit/integration/frontend build/docker smoke) has not been run yet after these changes.
