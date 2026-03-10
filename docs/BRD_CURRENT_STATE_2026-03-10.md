# CovenantIQ Business Requirements Document
## Current-State Edition

Date: 2026-03-10

## 1. Purpose
CovenantIQ is a commercial loan surveillance and governance platform used to monitor borrower covenant compliance, detect emerging risk, manage analyst workflow, and govern configuration changes around credit monitoring operations.

This document describes the business requirements implied by the codebase currently implemented in this repository as of March 10, 2026. It is a current-state BRD, not a target-state roadmap.

## 2. Business Problem
Credit monitoring teams need a controlled system for:
- tracking active commercial loans and their covenant packages
- ingesting periodic borrower financials
- calculating covenant performance consistently
- surfacing breaches and early warnings quickly
- documenting analyst collaboration and evidence
- controlling operational changes to monitoring logic and workflow behavior
- administering users, roles, imports, and outbound integrations

Manual spreadsheets and fragmented reviews increase operational risk, reduce auditability, and make it difficult to scale portfolio monitoring.

## 3. Product Goal
Provide a single application that combines:
- loan-level covenant surveillance
- portfolio-level risk visibility
- role-based operational workflow
- governance tooling for rules, workflows, releases, and webhook integrations

## 4. Stakeholders
- Analyst: creates and maintains loans, covenants, statements, comments, and supporting monitoring data
- Risk Lead: reviews portfolio risk, approves governance changes, and resolves higher-control actions
- Administrator: manages users, loan imports, and webhook integrations
- Credit Governance / Operations: uses workflow, release, and policy controls to manage monitoring configuration changes
- Engineering / Support: operates the platform, runtime modes, observability, and deployment

## 5. In-Scope Business Capabilities

### 5.1 Loan Monitoring
- Create, list, retrieve, and close commercial loans
- Maintain covenant packages per loan
- Update covenant thresholds, comparison direction, and severity
- Prevent invalid monitoring actions against closed loans

### 5.2 Financial Statement Intake
- Submit quarterly and annual financial statements manually
- Bulk import financial statements for an existing loan by file upload
- Admin preview and execute portfolio loan-import batches from CSV
- Preserve historical submissions through supersession instead of destructive overwrite

### 5.3 Covenant Evaluation
- Evaluate covenant compliance automatically on statement submission
- Support the following covenant types:
  - Current Ratio
  - Debt to Equity
  - DSCR
  - Interest Coverage
  - Tangible Net Worth
  - Debt to EBITDA
  - Fixed Charge Coverage
  - Quick Ratio
- Persist covenant results and expose them for analyst review

### 5.4 Early Warning Detection
- Generate early warnings for three consecutive current-ratio declines
- Generate near-threshold warnings
- Generate current-ratio volatility warnings
- Generate seasonal anomaly warnings for quarterly statements

### 5.5 Alert and Workflow Management
- Create breach and early-warning alerts from evaluation outcomes
- Manage alert lifecycle state transitions
- Back alert transitions with a published workflow definition and transition log
- Require role-appropriate action for lifecycle progression

### 5.6 Loan Collaboration and Evidence
- Add, list, and delete loan comments
- Upload, list, download, and delete PDF attachments against financial statements
- View loan-level activity history

### 5.7 Exception and Collateral Management
- Register collateral assets for a loan
- Create covenant exception requests
- Approve or expire exceptions through controlled actions
- Reflect approved exceptions in covenant decisioning behavior through ruleset logic

### 5.8 Portfolio and Reporting
- Produce loan risk summaries and risk details
- Produce portfolio-level summary views
- Export loan alerts and covenant results to CSV
- Surface global alerts in the UI across loans

### 5.9 Governance and Change Control
- Create and version rulesets
- Validate and publish ruleset versions
- Create and publish workflow definitions
- Submit, approve, release, and roll back change requests
- Record release audit actions

### 5.10 Integrations and Event Delivery
- Create and manage outbound webhook subscriptions
- Dispatch outbox events to subscribed webhook endpoints
- Retry failed webhook deliveries
- Track webhook delivery attempts and outcomes

### 5.11 Identity and Access Management
- Authenticate users with backend-issued JWT access and refresh tokens
- Enforce role-based authorization across functional areas
- Create, list, inspect, role-update, and deactivate users

### 5.12 Runtime Operations
- Expose runtime mode information for frontend behavior
- Support normal, demo, and test backend modes
- Seed demo/test sample data when enabled
- Provide health and API documentation endpoints

## 6. Out of Scope for the Current Build
- loan origination
- payment servicing
- core banking integration
- external accounting platform synchronization other than webhook/event patterns
- multi-tenant tenancy partitioning
- predictive credit scoring or ML-driven risk models
- production-grade external database migrations and persistent infrastructure automation

## 7. Functional Requirements

### FR-01 Loan Lifecycle
The system shall support creation, retrieval, listing, and closure of loans.

### FR-02 Covenant Administration
The system shall allow one covenant record per covenant type per loan and allow controlled covenant updates.

### FR-03 Statement Intake
The system shall support manual statement submission and file-based statement import for loan monitoring.

### FR-04 Portfolio Loan Import
The system shall allow administrators to preview, execute, and review batch loan imports.

### FR-05 Automated Evaluation
The system shall automatically calculate covenant metrics and evaluation results after statement submission.

### FR-06 Alerting
The system shall create breach alerts and early-warning alerts based on evaluation and trend-analysis outcomes.

### FR-07 Alert Workflow
The system shall maintain alert workflow state, transition permissions, and transition history.

### FR-08 Risk Views
The system shall provide both loan-level risk views and portfolio-level summary views.

### FR-09 Analyst Collaboration
The system shall support comments, attachments, and activity visibility for monitored loans.

### FR-10 Exceptions and Collateral
The system shall support collateral capture and covenant exception request/approval workflows.

### FR-11 Exports
The system shall export alert and covenant-result data in CSV form.

### FR-12 Governance Configuration
The system shall support creation, validation, publication, and review of rulesets and workflows.

### FR-13 Change Governance
The system shall support change request submission, approval, release creation, and rollback tracking.

### FR-14 Webhook Integration
The system shall support outbound webhook subscriptions, delivery tracking, and retry operations.

### FR-15 User Administration
The system shall support user administration and role management for administrators.

### FR-16 Access Control
The system shall enforce role-based permissions across analyst, risk lead, and admin user classes.

## 8. Business Rules
- Only authenticated users may access protected APIs when security is enabled.
- Roles drive access separation:
  - Analysts can manage monitoring data.
  - Risk Leads approve governance and higher-control actions.
  - Admins manage administrative surfaces.
- Covenant uniqueness is enforced by loan and covenant type.
- Financial monitoring history is preserved through supersession rather than delete-and-replace.
- Alert workflow transitions must match the published workflow definition.
- Resolution-style transitions may require additional metadata such as resolution notes.
- Only PDF statement attachments are accepted.
- File size limits apply to uploaded attachments.
- In normal runtime mode, placeholder secrets are not acceptable.

## 9. Non-Functional Requirements
- Deterministic financial calculations using precise decimal arithmetic
- Layered service architecture with transactional business flows
- RFC 7807 problem responses for API errors
- JWT-based stateless authentication with refresh support
- Correlation-aware request handling for traceability
- Structured logging suitable for operational troubleshooting
- Single-application deployment path for backend and frontend
- Responsive browser UI for operational users

## 10. Success Criteria for the Current Build
- A user can authenticate and complete the core monitoring lifecycle end to end.
- A user can review risk outputs, alerts, comments, documents, and activity from the UI.
- Risk and governance actions are separated by role.
- Rulesets, workflows, releases, and webhook subscriptions are manageable through the application.
- The platform can operate in normal mode and in sample-data demo/test modes.

## 11. Known Constraints
- The current datastore is H2 and is best aligned to development, demo, and test usage.
- The frontend is coupled to the current REST API and local token storage model.
- The current codebase supports governance concepts, but operational adoption still depends on process discipline outside the system.

## 12. Document Positioning
This file should be treated as the business description of the implemented product baseline on 2026-03-10.
