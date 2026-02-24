# CovenantIQ Business Requirements Document (BRD)

## 1. Project Overview

**Project name:** CovenantIQ  
**Domain:** Commercial banking / credit risk surveillance  
**Type:** Hackathon-style internship project  
**Goal:** Build an end-to-end system that helps credit risk analysts monitor commercial loan covenants, detect early warning signals, and review consolidated loan risk.

## 2. Business Problem

Credit teams often track covenant compliance manually across borrower financial statements. This introduces latency, inconsistency, and weak early-warning visibility.

CovenantIQ addresses this by:
- Centralizing loan and covenant data
- Automating covenant evaluation per statement
- Detecting warning trends before hard breaches
- Producing explainable risk outputs for analyst review

## 3. Stakeholders

- Primary user: Credit Risk Analyst
- Secondary user: Lending / relationship teams (read-only consumer of outputs)
- Project sponsors: Internship reviewers / technical mentors

## 4. Objectives and Success Criteria

### Objectives
- Deliver a working Java Spring Boot backend for loan risk surveillance
- Deliver a React + Tailwind frontend for analyst workflows
- Support deterministic, auditable covenant and warning evaluation
- Deploy as a single Docker container suitable for Dockploy

### Success Criteria
- Analyst can complete the full flow:
  1. Create loan
  2. Add covenant(s)
  3. Submit financial statement
  4. Observe generated results and alerts
  5. View risk summary and trend visuals
- APIs return consistent business/error contracts
- App runs via documented Docker build/run steps

## 5. Scope

### In Scope (Phase 1)
- Backend in Java (Spring Boot), layered architecture
- Domain entities: Loan, Covenant, FinancialStatement, CovenantResult, Alert
- Covenant evaluation and trend analysis services
- Risk summary endpoint
- Frontend (React + TypeScript + Tailwind) with core analyst flows
- UI-only mock login/session (no backend auth enforcement)
- Swagger/OpenAPI docs
- Seed demo data
- Unit tests + API integration tests

### Out of Scope (Phase 1)
- Real authentication/authorization
- Portfolio-wide multi-tenant governance features
- ML/AI scoring
- Production-grade distributed async processing

## 6. Functional Requirements

### FR-1 Loan Management
- Create loan
- List loans (paginated)
- Get loan by id
- Close loan lifecycle status (`ACTIVE` -> `CLOSED`)

### FR-2 Covenant Management
- Add covenant to loan
- Enforce one covenant per type per loan

### FR-3 Financial Statement Submission
- Accept quarterly and annual statements
- Upsert by loan + period key
- Soft-supersede prior version and derived artifacts
- Reject invalid denominator values with 422
- Block submissions for CLOSED loans with 409

### FR-4 Covenant Evaluation
- Compute ratios using BigDecimal:
  - Current Ratio = currentAssets / currentLiabilities
  - Debt-to-Equity = totalDebt / totalEquity
- Evaluate against covenant threshold + comparison type
- Persist CovenantResult
- Generate BREACH alerts on failed covenant conditions

### FR-5 Early Warning Trend Analysis
- Rule 1: 3 consecutive Current Ratio declines
- Rule 2: Near-threshold directional warning within 5%
- Evaluate declines separately by cadence stream (quarterly vs annual)
- Emit warning alerts per triggered condition

### FR-6 Risk Summary
- Return:
  - Total covenants
  - Number breached
  - Active warnings (latest evaluation cycle only)
  - Overall risk level
- Risk ladder:
  - HIGH if any HIGH severity breach
  - MEDIUM if any breach/warning without HIGH breach
  - LOW otherwise

### FR-7 Frontend Analyst Experience
- Mock login page and protected app routes
- Loan list and detail views
- Add covenant form
- Submit statement form
- Results/alerts tables (filter + pagination)
- Risk summary display
- Trend chart(s) + tabular detail

## 7. Non-Functional Requirements

- Java 21 LTS compatibility
- Deterministic financial math using BigDecimal and explicit rounding
- API versioning under `/api/v1`
- RFC7807 error model (`application/problem+json`)
- UTC normalization for persisted and returned timestamps
- Clean layered architecture, SOLID-aligned design
- Single-container deploy path

## 8. Key Business Rules

- Controllers contain no business logic
- One covenant type per loan
- Duplicate period submissions are upserts, not duplicates
- Superseded records remain for audit but are excluded from active summaries by default
- Breach and warning may both be emitted in same cycle when both rules trigger

## 9. Assumptions

- Hackathon environment prioritizes end-to-end correctness over advanced enterprise controls
- No external identity provider required for phase 1
- Data volume is moderate; pagination still required for list endpoints

## 10. Acceptance Criteria (High-Level)

- All required APIs implemented and callable
- Full flow works from frontend without direct DB intervention
- Error responses are consistent and actionable
- Tests cover critical business flows and rule boundaries
- Dockerized app launches and serves both API and UI from one container

