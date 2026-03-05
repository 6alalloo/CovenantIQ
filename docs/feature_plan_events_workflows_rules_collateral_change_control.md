# Feature Plan: Events, Workflows, Rules, Collateral, and Change Control

## 1. Summary
This plan defines a production-oriented expansion for CovenantIQ focused on features `#1, #2, #3, #4, #7`:
- Event delivery: **Outbox + Webhooks**
- Workflow engine: **Internal state machine**
- Decisioning: **No-code rule versions with publish controls**
- Risk context: **Collateral + covenant exceptions as first-class entities**
- Governance: **Maker-checker change control with staged release and rollback**

The target outcome is a configurable, auditable, integration-ready risk monitoring platform that preserves CovenantIQ's current core strengths in post-disbursement covenant surveillance.

## 2. Goals and Success Criteria
### Goal
Replace hardcoded operational logic (especially lifecycle transitions and policy logic) with managed configuration, controlled releases, and external integration support.

### Success Criteria
1. Domain events are reliably emitted and delivered to subscribers.
2. Alert and covenant lifecycle transitions are definition-driven, not hardcoded.
3. Business users can draft, test, and publish policy/rule versions with approval.
4. Collateral and active exceptions materially influence evaluation outcomes and are auditable.
5. Configuration changes are governed through maker-checker + staged release + rollback.
6. Initial implementation delivers both Phase A and Phase B together with UI and tests.

## 3. Scope
### In Scope
- Transactional outbox event store, dispatcher, webhook subscriptions, signature verification, retries, and dead-letter states.
- Internal workflow definition and runtime engine for alert lifecycle and future process entities.
- Rule set registry, rule versioning, validation harness, and controlled publish process.
- Collateral asset/valuation management and covenant exception/waiver lifecycle.
- Change request, approval, release, and rollback framework for operational configuration.

### Out of Scope
- Kafka-first distributed event streaming in this phase.
- Camunda/BPMN or Temporal orchestration in this phase.
- Fully autonomous approval or decisioning.
- Multi-tenant isolation redesign.

## 4. Architecture
### 4.1 High-Level Components
1. **Domain Services** (existing): loan, covenant, statement, alert, risk services.
2. **Event Outbox Module** (new): persists events in same transaction as domain writes.
3. **Webhook Dispatcher** (new): polls outbox, signs payloads, pushes to subscriptions, retries on failure.
4. **Workflow Engine** (new): enforces transition rules, role guards, and required action metadata.
5. **Rules Engine Layer** (new): evaluates active published rule version by ruleset key.
6. **Collateral/Exception Context Provider** (new): injects collateral and waiver state into evaluation context.
7. **Change Control Module** (new): governs draft/approval/release/rollback lifecycle for configuration artifacts.

### 4.2 Runtime Flow
1. Domain command executes (for example statement submission or alert transition).
2. Business data and `event_outbox` row commit in a single transaction.
3. Dispatcher picks event, signs payload, sends webhook deliveries, records attempts.
4. Workflow transition requests validate role + allowed path + required fields.
5. Rule evaluator loads active ruleset version and computes results.
6. Collateral and active exceptions alter breach/escalation outcomes with explicit reason codes.
7. Change control gates any publish/release action and records immutable audit snapshots.
8. Exception expiry is enforced via hybrid strategy: scheduled daily expiry job plus lazy expiry checks on relevant reads/writes.

## 5. Data Model Additions
## 5.1 Eventing
- `event_outbox`
  - `id`, `event_id` (UUID), `aggregate_type`, `aggregate_id`, `event_type`, `payload_json`, `headers_json`
  - `status` (`PENDING|IN_PROGRESS|DELIVERED|DEAD_LETTER`)
  - `attempt_count`, `next_attempt_at`, `created_at`, `updated_at`
- `webhook_subscription`
  - `id`, `name`, `endpoint_url`, `secret_ref`, `event_filters_json`, `active`, `created_by`, `created_at`
- `webhook_delivery`
  - `id`, `event_outbox_id`, `subscription_id`, `attempt_no`, `response_status`, `response_body_hash`
  - `latency_ms`, `delivery_status`, `error_code`, `attempted_at`

### 5.2 Workflows
- `workflow_definition`
  - `id`, `entity_type`, `name`, `version`, `status` (`DRAFT|PUBLISHED|RETIRED`), `created_by`, `created_at`
- `workflow_state`
  - `id`, `workflow_definition_id`, `state_code`, `is_initial`, `is_terminal`
- `workflow_transition`
  - `id`, `workflow_definition_id`, `from_state`, `to_state`, `allowed_roles_csv`, `required_fields_json`, `guard_expression`
- `workflow_instance`
  - `id`, `entity_type`, `entity_id`, `workflow_definition_id`, `current_state`, `started_at`, `updated_at`
- `workflow_transition_log`
  - `id`, `workflow_instance_id`, `from_state`, `to_state`, `actor`, `reason`, `metadata_json`, `timestamp_utc`

### 5.3 Rules
- `ruleset`
  - `id`, `key`, `name`, `domain` (`COVENANT_EVAL`), `owner_role`, `created_by`, `created_at`
- `ruleset_version`
  - `id`, `ruleset_id`, `version`, `status` (`DRAFT|VALIDATED|PUBLISHED|ARCHIVED`)
  - `definition_json`, `schema_version`, `change_summary`, `created_by`, `approved_by`, `published_at`
- `ruleset_test_case`
  - `id`, `ruleset_version_id`, `input_json`, `expected_output_json`, `actual_output_json`, `pass`, `executed_at`
- `ruleset_publish_audit`
  - `id`, `ruleset_id`, `from_version`, `to_version`, `actor`, `timestamp_utc`, `reason`

### 5.4 Collateral and Exceptions
- `collateral_asset`
  - `id`, `loan_id`, `asset_type`, `description`, `nominal_value`, `haircut_pct`, `net_eligible_value`, `lien_rank`, `currency`, `effective_date`
- `collateral_valuation`
  - `id`, `collateral_asset_id`, `valuation_date`, `valued_amount`, `method`, `source`, `confidence`
- `covenant_exception`
  - `id`, `loan_id`, `covenant_id`, `exception_type` (`WAIVER|OVERRIDE`), `reason`, `effective_from`, `effective_to`
  - `status` (`REQUESTED|APPROVED|EXPIRED|REJECTED`), `requested_by`, `approved_by`, `approved_at`, `controls_json`

### 5.5 Change Control
- `change_request`
  - `id`, `type` (`RULESET|WORKFLOW|INTEGRATION_CONFIG`), `status` (`DRAFT|SUBMITTED|APPROVED|REJECTED|RELEASED|ROLLED_BACK`)
  - `requested_by`, `requested_at`, `approved_by`, `approved_at`, `justification`
- `change_request_item`
  - `id`, `change_request_id`, `artifact_type`, `artifact_id`, `from_version`, `to_version`, `diff_json`
- `release_batch`
  - `id`, `change_request_id`, `release_tag`, `released_by`, `released_at`, `rollback_of_release_id`
- `release_audit`
  - `id`, `release_batch_id`, `action`, `actor`, `details_json`, `timestamp_utc`

## 6. API Additions
## 6.1 Integrations
- `POST /api/v1/integrations/webhooks`
- `GET /api/v1/integrations/webhooks`
- `PATCH /api/v1/integrations/webhooks/{id}`
- `GET /api/v1/integrations/webhooks/{id}/deliveries`

### 6.2 Workflows
- `POST /api/v1/workflows/definitions`
- `GET /api/v1/workflows/definitions`
- `POST /api/v1/workflows/definitions/{id}/publish`
- `GET /api/v1/workflows/instances/{entityType}/{entityId}`
- `POST /api/v1/workflows/instances/{id}/transition`

### 6.3 Rules
- `POST /api/v1/rulesets`
- `POST /api/v1/rulesets/{id}/versions`
- `POST /api/v1/rulesets/{id}/versions/{version}/validate`
- `POST /api/v1/rulesets/{id}/versions/{version}/publish`
- `GET /api/v1/rulesets/{id}/versions`

### 6.4 Collateral and Exceptions
- `POST /api/v1/loans/{loanId}/collaterals`
- `GET /api/v1/loans/{loanId}/collaterals`
- `POST /api/v1/loans/{loanId}/exceptions`
- `PATCH /api/v1/exceptions/{id}/approve`
- `PATCH /api/v1/exceptions/{id}/expire`

### 6.5 Change Control
- `POST /api/v1/change-requests`
- `PATCH /api/v1/change-requests/{id}/approve`
- `POST /api/v1/releases`
- `POST /api/v1/releases/{id}/rollback`

### 6.6 API Behavior Rules
- Use RFC7807 error responses with machine-readable `code` values.
- Workflow transition conflicts return `409` with transition diagnostic details.
- Publish endpoints require explicit `reason` field.
- Rollback endpoints require target release reference and justification.
- Webhook delivery timeout is 5 seconds per attempt.
- Webhook integrations APIs are `ADMIN`-only.

## 7. UI Impacts
Add the following sections to frontend IA:
1. `Integrations`
   - Webhook registration/editing, secret rotation, event filter selection, delivery logs, retry actions.
2. `Workflow Designer`
   - State/transition matrix, role guard editor, publish history, draft diff.
3. `Policy Studio`
   - Ruleset list, draft editor, test harness, validation report, publish flow.
4. `Collateral & Exceptions` (loan-level tabs)
   - Asset registry, valuation timeline, exception requests, approval/expiry controls, reminders.
5. `Change Control`
   - Change request queue, approvals, release timeline, rollback actions, immutable audit viewer.

## 8. RBAC and Governance
- `ADMIN`
  - Full management across integrations, workflows, rules, collateral exceptions, and change releases.
- `RISK_LEAD`
  - Approve exceptions; approve ruleset/workflow publishes; approve releases.
- `ANALYST`
  - Draft rules/workflows, submit change requests, create exception requests, view integration status.

Temporary implementation policy:
- `ADMIN` direct publish is allowed for ruleset versions during early implementation.
- Full maker-checker remains required for change-control release and rollback flows.

Sensitive actions must store:
- Actor
- Timestamp
- Reason
- Before/after diff snapshot
- Correlation ID

## 9. Rollout Plan
### Wave 1: Phase A + Phase B (Delivered Together)
- Implement outbox schema and reliable dispatcher.
- Add webhook CRUD and delivery log APIs.
- Emit events from statement submit, covenant evaluation, alert lifecycle changes.
- Introduce workflow definitions and instances.
- Migrate alert status transitions from hardcoded logic to workflow transitions.
- Keep backward compatibility for current statuses (`OPEN`, `ACKNOWLEDGED`, `UNDER_REVIEW`, `RESOLVED`).
- Deliver backend and frontend UI together for Integrations and Alert Workflow surfaces.

### Phase C: Ruleset Versioning and Publish
- Add ruleset and version APIs.
- Implement validation harness and publish controls.
- Route covenant evaluation through active published ruleset.

### Phase D: Collateral and Exceptions
- Add collateral asset/valuation and exception lifecycle.
- Join exception/collateral context in breach logic and risk summaries.
- Emit exception approval/expiry events.
- Approved exception behavior downgrades breach outcomes to warning (does not fully suppress).
- Include collateral valuation history in first implementation.

### Phase E: Change Control and Releases
- Add maker-checker workflow for configuration artifacts.
- Add release and rollback endpoints.
- Enforce publish through approved change requests only.

## 10. Observability and Operations
### Metrics
- `outbox_pending_count`
- `outbox_lag_seconds`
- `webhook_delivery_success_rate`
- `webhook_retry_count`
- `webhook_dead_letter_count`
- `workflow_transition_failure_count`
- `ruleset_validation_failure_count`

### Logging
- Include `correlationId`, `eventId`, `workflowInstanceId`, `changeRequestId` in structured logs.
- Log every approval, publish, release, rollback action.

### Health/Readiness
- Extend readiness with dispatcher heartbeat and outbox depth threshold checks.
- Alert on sustained dead-letter growth or dispatch lag breaches.

## 11. Acceptance Tests
### Unit
1. Outbox row is created in every event-emitting transaction.
2. Retry/backoff logic advances state and marks dead-letter at max attempts.
3. Workflow guard logic enforces role and required fields.
4. Rule parser/validator handles malformed and edge condition expressions.
5. Exception validity windows and overlap constraints are enforced.

### Integration
1. Statement submission emits expected idempotent events exactly once.
2. Invalid workflow transition returns `409` with transition diagnostics.
3. Publishing a ruleset version changes subsequent evaluation outcomes.
4. Approved exception downgrades breach to warning with explicit reason code.
5. Rollback reactivates prior rule/workflow version without data corruption.

### E2E
1. Admin configures webhook and receives signed payload deliveries.
2. Analyst drafts policy; Risk Lead approves; release activates changes.
3. Exception expiry automatically re-enables standard breach detection.

## 12. Dependencies and Constraints
- Current architecture remains single-container for initial rollout.
- H2 remains the primary runtime datastore for this phase to preserve single-container simplicity.
- Event signature secrets should be externally managed in non-dev environments.
- Backward compatibility required for existing alert lifecycle and frontend pages during migration.

## 13. Explicit Defaults Chosen
1. Eventing default: Outbox + Webhooks (not Kafka-first).
2. Workflow default: Internal state machine (not BPMN/Temporal).
3. Decisioning default: No-code ruleset versioning with approval gates.
4. Governance default: full change-control objects and release/rollback are in-scope from initial implementation.
5. Rollout default: begin by implementing both Phase A and Phase B together.

## 14. Locked Preferences and Agreements (Q1-Q34)
1. Implement both Phase A and Phase B initially.
2. Keep H2 and single-container deployment as top priority.
3. Keep backward compatibility for existing alert status API while migrating internals to workflow engine.
4. Emit all important domain events (catalog listed below).
5. Include `eventVersion` from day one.
6. Use in-app dispatcher only for now.
7. Retry policy approved: exponential backoff with max-attempt dead-letter behavior.
8. Dead-letter events are manually retryable via admin API in v1.
9. Webhook signing to use secure default headers and HMAC-SHA256.
10. Webhook timeout is 5 seconds per attempt.
11. Subscription filters support event type + severity + loan/portfolio filters.
12. Store encrypted webhook secret in DB for now.
13. Enforce idempotency with DB dedupe (`event_id + subscription_id`).
14. Integration/webhook APIs are `ADMIN`-only.
15. Workflow first scope is alert workflow only.
16. Workflow definition format is internal JSON schema (not BPMN).
17. Transition guards are simple built-in predicates.
18. Transition required fields follow sensible defaults (`RESOLVED` requires resolution notes, etc.).
19. Ruleset versioning first target is covenant evaluation.
20. Rules representation is JSON DSL in v1.
21. Temporary `ADMIN` direct publish is allowed during early implementation.
22. Full change control model is required (not lightweight-only).
23. Collateral implementation includes valuation history from first release.
24. Active approved exceptions downgrade breaches to warnings.
25. Exception lifecycle states: `REQUESTED -> APPROVED/REJECTED -> EXPIRED`.
26. Exception expiry strategy: hybrid scheduled job + lazy expiry checks.
27. Config-change audit is immutable append-only.
28. No hard SLA targets were mandated for this phase.
29. Include frontend UI implementation in this work (not backend-only).
30. Follow TDD for implementation and test depth decisions.
31. No feature flags required pre-deployment.
32. Deliver as one large PR.
33. Update this doc with progress after each completed feature.
34. Confirmed non-goals for this implementation: no Kafka, Camunda, Temporal, or multi-tenant redesign.

### 14.1 Approved Initial Event Catalog
- `LoanCreated`, `LoanClosed`
- `CovenantCreated`, `CovenantUpdated`
- `FinancialStatementSubmitted`, `FinancialStatementSuperseded`, `FinancialStatementBulkImported`
- `CovenantEvaluated`, `CovenantBreachDetected`
- `AlertCreated`, `AlertStatusChanged`, `AlertSuperseded`
- `ExceptionRequested`, `ExceptionApproved`, `ExceptionRejected`, `ExceptionExpired`
- `WorkflowDefinitionPublished`, `RulesetVersionPublished`, `ReleaseCreated`, `ReleaseRolledBack`

## 15. Implementation Progress Log
### 2026-03-05: Initial End-to-End Delivery
Completed backend and frontend delivery for the feature plan scope with production-ready baseline behavior.

Backend delivered:
1. Eventing (Phase A)
- Implemented transactional outbox persistence and repositories.
- Implemented webhook subscription CRUD APIs (`/api/v1/integrations/webhooks`), delivery log API, and dead-letter retry API.
- Implemented scheduled dispatcher with HMAC-SHA256 signing, 5s timeout, exponential backoff, and dead-letter status.
- Wired outbox emission for loan, covenant, statement, covenant evaluation, alert, workflow publish, ruleset publish, exception lifecycle, and release lifecycle events.

2. Workflow engine (Phase B)
- Implemented workflow definition/state/transition/instance/log data model and APIs:
  - `POST/GET /api/v1/workflows/definitions`
  - `POST /api/v1/workflows/definitions/{id}/publish`
  - `GET /api/v1/workflows/instances/{entityType}/{entityId}`
  - `POST /api/v1/workflows/instances/{id}/transition`
- Added default published alert workflow bootstrap.
- Migrated alert status transition enforcement to workflow runtime.
- Implemented `409` transition conflicts with diagnostics payload.

3. Ruleset versioning (Phase C)
- Implemented ruleset/ruleset-version/testcase/publish-audit data model and APIs:
  - `POST /api/v1/rulesets`
  - `POST /api/v1/rulesets/{id}/versions`
  - `POST /api/v1/rulesets/{id}/versions/{version}/validate`
  - `POST /api/v1/rulesets/{id}/versions/{version}/publish`
  - `GET /api/v1/rulesets/{id}/versions`
- Added bootstrap default covenant-evaluation ruleset and published version.
- Routed covenant breach decisioning through active published ruleset.

4. Collateral and exceptions (Phase D)
- Implemented collateral asset and valuation-capable model foundation plus loan collateral APIs:
  - `POST/GET /api/v1/loans/{loanId}/collaterals`
- Implemented covenant exception lifecycle APIs:
  - `POST/GET /api/v1/loans/{loanId}/exceptions`
  - `PATCH /api/v1/exceptions/{id}/approve`
  - `PATCH /api/v1/exceptions/{id}/expire`
- Implemented hybrid expiry strategy:
  - Scheduled daily expiry job.
  - Lazy active-window checks during covenant evaluation.
- Implemented approved exception downgrade behavior (breach -> warning alert path with explicit reason code).

5. Change control and releases (Phase E)
- Implemented change request/release/rollback data model and APIs:
  - `POST/GET /api/v1/change-requests`
  - `PATCH /api/v1/change-requests/{id}/approve`
  - `POST/GET /api/v1/releases`
  - `POST /api/v1/releases/{id}/rollback`
- Added release audit entries and lifecycle events (`ReleaseCreated`, `ReleaseRolledBack`).

6. API behavior/gov controls
- Preserved RFC7807 responses; added machine-readable `code` property to problem details.
- Enforced publish endpoints `reason` request contract (workflow and ruleset publish).
- Enforced rollback request justification contract.
- Enforced `ADMIN`-only webhook integrations APIs.

Frontend delivered:
1. New top-level app areas and navigation
- `Integrations`
- `Workflow Designer`
- `Policy Studio`
- `Change Control`

2. Loan-level additions
- Added `Collateral` tab under loan details with:
  - collateral registry actions
  - covenant exception request/approve/expire actions

3. Frontend integration
- Added typed API client bindings and domain types for all new backend endpoints.

Validation performed:
1. Backend compile/package: `mvn -q -DskipTests package` (pass).
2. Frontend production build: `cmd /c npm run build` (pass).
3. Focused backend integration smoke: `mvn -q -Dtest=HealthEndpointIntegrationTest test` (pass).

Known follow-ups:
1. Full existing backend test suite (`mvn -q test`) has legacy assertion mismatches after workflow-governed alert transitions and expanded startup behavior; targeted compatibility updates are still required for all historical integration tests.
2. Collateral valuation timeline UI is scaffolded at model level; dedicated valuation CRUD UI/API endpoints can be expanded in a follow-up.
