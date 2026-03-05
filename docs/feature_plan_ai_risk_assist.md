# Feature Plan: AI Risk Assist (Advisory, Batch-First)

## 1. Summary
This plan defines CovenantIQ feature `#10` as a dedicated implementation track:
- Runtime: **Batch scoring in Spring**
- Policy: **Advisory-only AI** (no autonomous decisioning)

The objective is to provide explainable breach-risk signals for analysts and risk leads while preserving human ownership of final actions.

## 2. Goals and Success Criteria
### Goal
Introduce explainable breach-risk predictions across active loans to improve prioritization and early intervention.

### Success Criteria
1. Every active loan receives `30/90/180` day breach-risk scores.
2. UI displays score, confidence, and top contributing factors.
3. AI outputs are fully auditable by model/version and feature snapshot.
4. AI never auto-resolves, auto-escalates, or auto-mutates alert/workflow state.

## 3. Scope
### In Scope
- Spring-based feature pipeline.
- Nightly batch scoring + incremental rescore trigger on new statements.
- Model registry with controlled activation of one active model version.
- Explainability outputs for per-loan score interpretation.
- Analyst feedback capture for model improvement loop.

### Out of Scope
- Real-time online training.
- Autonomous decisioning and state mutation.
- External AI provider integration for v1.
- Separate Python inference service for v1.

## 4. Architecture
### 4.1 High-Level Components
1. **Feature Builder Service**
   - Derives model inputs from loans, covenants, statements, alerts, trends, and collateral/exception context when available.
2. **Model Registry Service**
   - Stores model metadata and activation state.
3. **Batch Scoring Scheduler**
   - Nightly full-portfolio scoring job.
4. **Incremental Scoring Trigger**
   - On statement submit event, re-score impacted loan.
5. **Explainability Generator**
   - Produces ordered top-factor contributions and human-readable reasons.
6. **AI API Layer**
   - Exposes per-loan and portfolio AI insights.
7. **Feedback Capture Service**
   - Stores analyst feedback linked to score/model snapshot.

### 4.2 Runtime Flow
1. Scheduler selects active loans.
2. Feature Builder computes immutable snapshot per loan.
3. Active model artifact is loaded from registry.
4. Scorer produces horizon scores and confidence values.
5. Explainability module computes signed contributions and reason strings.
6. Persist `ai_score`, `ai_contribution`, and `ai_feature_snapshot`.
7. APIs serve latest valid scores plus staleness metadata.
8. New statement events enqueue incremental re-score for affected loan.

## 5. Data Model Additions
- `ai_model`
  - `id`, `name`, `version`, `algorithm`, `artifact_uri`, `feature_schema_hash`, `status` (`DRAFT|ACTIVE|RETIRED`)
  - `created_by`, `created_at`, `activated_by`, `activated_at`
- `ai_feature_snapshot`
  - `id`, `loan_id`, `feature_json`, `feature_schema_hash`, `generated_at_utc`
- `ai_score`
  - `id`, `loan_id`, `horizon_days` (`30|90|180`), `score`, `risk_band`, `confidence`
  - `model_id`, `feature_snapshot_id`, `generated_at_utc`, `staleness_status`
- `ai_contribution`
  - `id`, `ai_score_id`, `feature_name`, `contribution_value`, `direction` (`POSITIVE|NEGATIVE`), `reason_text`, `rank_order`
- `ai_feedback`
  - `id`, `loan_id`, `ai_score_id`, `user_action` (`HELPFUL|NOT_HELPFUL|NEEDS_REVIEW`), `comment`, `submitted_by`, `submitted_at`

## 6. API Additions
- `GET /api/v1/loans/{loanId}/ai-risk`
  - Returns latest 30/90/180 risk panel + explainability + model metadata.
- `GET /api/v1/portfolio/ai-risk-summary`
  - Aggregate counts by risk band and horizon, score freshness indicators.
- `GET /api/v1/portfolio/ai-risk-top`
  - Ranked high-risk list with filters (`horizon`, `band`, `minScore`).
- `POST /api/v1/admin/ai/models`
  - Register model metadata and artifact location.
- `POST /api/v1/admin/ai/models/{id}/activate`
  - Atomically make model active and archive prior active model.
- `GET /api/v1/admin/ai/models`
  - Model catalog and activation history.
- `POST /api/v1/loans/{loanId}/ai-feedback`
  - Persist analyst feedback on AI usefulness.

### 6.1 Explainability Response Contract
Per horizon entry includes:
1. `score` (0-100)
2. `riskBand` (`LOW|MEDIUM|HIGH`)
3. `horizonDays`
4. `confidence`
5. `topFactors[]` (feature, signed impact, reason)
6. `modelVersion`
7. `featureSnapshotId`
8. `generatedAtUtc`
9. `disclaimer` = "Advisory signal; final decision remains human."

## 7. UI Impacts
1. **Loan Detail**
   - New `AI Risk Assist` card with `30/90/180` tabs, confidence, trend sparkline, top drivers.
2. **Portfolio**
   - AI high-risk watchlist table and freshness/coverage badges.
3. **Feedback UX**
   - Action controls: `Helpful`, `Not Helpful`, `Needs Review` with optional notes.
4. **Admin AI Page**
   - Model list, activation history, active model marker, drift/freshness indicators.

## 8. RBAC and Governance
- `ADMIN`
  - Register/activate/retire models; view all AI operational telemetry.
- `RISK_LEAD`
  - View full AI insights and portfolio risk ranking.
- `ANALYST`
  - View loan AI insights and submit feedback.

Governance controls:
- Every score references exact model version and feature snapshot.
- Activation requires actor identity and reason.
- AI outputs cannot invoke workflow transition APIs directly.

## 9. Rollout Plan
### Phase 1: Data and Registry Foundation
- Create AI model/score/snapshot/contribution/feedback schemas.
- Add admin model APIs and model activation controls.

### Phase 2: Batch Scoring MVP
- Implement nightly scheduler for full portfolio scoring.
- Serve AI score APIs for loan and portfolio views.

### Phase 3: Incremental Re-Score
- Hook statement submission to loan-level re-score queue.
- Add staleness metadata in API and UI.

### Phase 4: Explainability and Feedback Loop
- Add top-factor reasoning in responses.
- Add feedback capture and reporting for model review.

### Phase 5: Hardening
- Add drift/freshness monitoring, fallback behaviors, and runbook procedures.

## 10. Observability and Operations
### Metrics
- `ai_scoring_job_duration_seconds`
- `ai_scoring_job_failures_total`
- `ai_scores_generated_total`
- `ai_score_staleness_count`
- `ai_model_activation_total`
- `ai_feedback_count`

### Logging
- Include `correlationId`, `modelId`, `modelVersion`, `featureSnapshotId`, `loanId`.
- Log model activation, batch job start/end, scoring failures, and fallback usage.

### Health/Readiness
- Readiness includes active model presence and scoring scheduler heartbeat.
- Alert when score freshness SLA is violated or scoring failure rate spikes.

## 11. Acceptance Tests
### Unit
1. Feature Builder outputs deterministic vectors for fixed inputs.
2. Risk band mapping from numeric score is deterministic and bounded.
3. Contribution ranking is stable and ordered by absolute impact.

### Integration
1. New statement submission triggers incremental re-score for affected loan.
2. Model activation switches inference target atomically.
3. Missing/invalid model path returns last valid score with explicit staleness metadata.

### E2E
1. Analyst sees updated AI panel after new financial statement submission.
2. Feedback submission is persisted and visible in admin analytics.
3. AI advisory-only rule is preserved (no direct workflow/alert state mutations).

## 12. Risk, Compliance, and Controls
- Minimize PII in feature set and scoring outputs.
- Persist model and score lineage for audit.
- Restrict model management endpoints to authorized roles.
- Include stale-score warning and model-not-available fallback semantics.
- Keep explainability human-readable to support governance and challenge processes.

## 13. Dependencies and Constraints
- Current Spring Boot single-container baseline remains valid for v1.
- PostgreSQL + migration tooling strongly recommended before large-scale operation.
- Model artifact storage path and access controls must be environment-managed.
- This plan assumes no external provider data flow in v1.

## 14. Explicit Defaults Chosen
1. AI runtime default: batch scoring in Spring.
2. AI usage default: advisory only, never auto-decisioning.
3. Delivery default: nightly full scoring + event-driven incremental re-score.
4. Governance default: model activation is explicit, auditable, and role-restricted.
5. Explainability default: top-factor contributions always returned with every score.
