# CovenantIQ Frontend IA and Page Plan (Feb 2026 Conventions)

## Summary
This plan defines a complete, implementation-ready frontend architecture for all requested pages using `/app/*` nested protected routes, core-first delivery, and explicit backend API gap closure.

It is grounded in the current codebase state:
- Mock-auth SPA with only login/dashboard routes.
- Backend JWT + RBAC and Phase 2 APIs are available.
- Remaining frontend work is primarily IA, route expansion, feature screens, and API integration.

## Locked Decisions
1. Scope: `Frontend + API gaps`.
2. Canonical protected routing: `/app/*`.
3. Delivery model: `Core-first phases`.
4. Loan detail information architecture: `Nested tab routes`.
5. Reports/Settings depth for first pass: `Operational MVP`.
6. Typography: no Inter; use a distinctive enterprise stack with a dedicated numeric mono face.
7. Electric blue is reserved for interactive states only (focus, active, selected, CTA).
8. Layout density target is roomier enterprise spacing, not compact terminal density.
9. Charts use multicolor palettes where category clarity benefits comprehension.
10. Sidebar gets branded visual treatment (identity rail/highlight treatment).
11. Background is layered/textured, not flat.
12. Include light mode tokens in the design system plan (dark remains default for v1).

## Design Direction (Locked for Refinement)
### Aesthetic Theme
1. Direction: `Industrial Dark Command Center`.
2. Visual posture: minimal shadows, high contrast surfaces, subtle edge definition, restrained radii.
3. Memorability anchor: branded left rail + electric-blue interaction language over layered dark atmosphere.

### Typography System
1. Display/UI font: `Sora` (headings, navigation, labels).
2. Body font: `Manrope` (forms, tables, explanatory copy).
3. Numeric/financial font: `JetBrains Mono` (KPIs, ratios, currency, tabular figures).
4. Typographic behavior: use tabular numerals for KPI cards and tables where alignment matters.

### Spacing, Radius, and Depth
1. Spacing scale target: roomier enterprise (`8, 12, 16, 24, 32, 40`).
2. Radius policy: small radii only (`4px` inputs/badges, `6px` cards/modals where needed).
3. Depth policy: rely on border and tonal contrast first; shadow only for overlay separation.

### Color and Token Foundation
1. Accent usage policy: electric blue appears on interactive states only, not as broad card/chart fill.
2. Risk semantics remain explicit: red (breach/high), amber (warning/medium), green (healthy/low).
3. Background treatment: layered gradient + low-opacity grain/noise texture for subtle atmosphere.
4. Multicolor chart policy: use category palettes when it improves scanning clarity; keep risk colors semantically protected.

### Initial Token Draft (to implement in CSS variables)
```css
/* Dark (default) */
--bg-app: #0a0a0f;
--bg-surface-1: #11131a;
--bg-surface-2: #171a22;
--border-default: #2a2f3a;
--text-primary: #f5f7fa;
--text-secondary: #9aa4b2;
--accent-interactive: #2f7bff;
--risk-high: #ff4d4f;
--risk-medium: #ffb020;
--risk-low: #22c55e;
--chart-c1: #3b82f6;
--chart-c2: #14b8a6;
--chart-c3: #f59e0b;
--chart-c4: #ef4444;
--chart-c5: #a855f7;
```

```css
/* Light (planned tokens for future mode support) */
--bg-app: #f4f6fb;
--bg-surface-1: #ffffff;
--bg-surface-2: #eef2f8;
--border-default: #d3dbe7;
--text-primary: #111827;
--text-secondary: #4b5563;
--accent-interactive: #1d4ed8;
--risk-high: #dc2626;
--risk-medium: #d97706;
--risk-low: #16a34a;
--chart-c1: #2563eb;
--chart-c2: #0d9488;
--chart-c3: #ca8a04;
--chart-c4: #dc2626;
--chart-c5: #7c3aed;
```

## Route Map (Canonical)
```txt
/login

/app
/app/dashboard
/app/portfolio
/app/loans
/app/loans/:loanId/overview
/app/loans/:loanId/statements
/app/loans/:loanId/results
/app/loans/:loanId/alerts
/app/loans/:loanId/documents
/app/loans/:loanId/comments
/app/loans/:loanId/activity
/app/alerts
/app/reports
/app/admin/users
/app/settings
```

## Role Access Matrix
| Page | ANALYST | RISK_LEAD | ADMIN |
|---|---|---|---|
| Dashboard | Yes | Yes | Yes |
| Portfolio Overview | No | Yes | Yes |
| Loans List | Yes | Yes | Yes |
| Loan Detail (all tabs) | Yes | Yes | Yes |
| Alert Center | Yes | Yes | Yes |
| Reports & Export | Yes | Yes | Yes |
| User Management | No | No | Yes |
| Settings | Yes | Yes | Yes |

## Page-by-Page Functional Spec
| Route | Purpose | Key UI Elements | Data + Actions |
|---|---|---|---|
| `/login` | JWT entry point | Username, password, submit, auth errors, lockout message | `POST /auth/login`, token store, redirect to `/app/dashboard` |
| `/app/dashboard` | Daily operational cockpit | KPI cards, my open alerts, recent covenant results, quick actions, loan quick switcher | `GET /loans`, `GET /loans/{id}/risk-summary`, `GET /loans/{id}/alerts`, `GET /loans/{id}/covenant-results` |
| `/app/portfolio` | Portfolio risk oversight | Total active loans, risk distribution, open/under-review counts, trend area, risk buckets table | `GET /portfolio/summary`, drill-through links |
| `/app/loans` | Searchable loan directory | Search input, status/risk filters, sortable table, bulk select, export CTA, row actions | `GET /loans`, per-row risk hydration, row navigation |
| `/app/loans/:id/overview` | Loan snapshot and covenant config | Borrower card, covenant list, covenant create form, risk detail panel | `GET /loans/{id}`, `GET /loans/{id}/risk-details`, `POST /loans/{id}/covenants`, `GET /loans/{id}/covenants` (new) |
| `/app/loans/:id/statements` | Financial lifecycle | Statement history table, single submit form, CSV/XLSX bulk import panel | `POST /loans/{id}/financial-statements`, `POST /loans/{id}/financial-statements/bulk-import`, `GET /loans/{id}/financial-statements` (new) |
| `/app/loans/:id/results` | Covenant evaluation timeline | Timeline/table, sparkline, filters by type/status/date | `GET /loans/{id}/covenant-results` |
| `/app/loans/:id/alerts` | Loan alert operations | Alert list, status chips, transition actions, resolution-notes modal | `GET /loans/{id}/alerts`, `PATCH /alerts/{alertId}/status` |
| `/app/loans/:id/documents` | Statement attachments | Statement selector, file uploader, attachments table, download/delete | `GET /financial-statements/{id}/attachments`, `POST /financial-statements/{id}/attachments`, `GET /attachments/{id}`, `DELETE /attachments/{id}` |
| `/app/loans/:id/comments` | Collaboration notes | Comment composer, comments list, delete action where permitted | `GET /loans/{id}/comments`, `POST /loans/{id}/comments`, `DELETE /loans/{id}/comments/{commentId}` |
| `/app/loans/:id/activity` | Audit trail | Activity feed table, event/type/date filters, actor badges | `GET /loans/{id}/activity` |
| `/app/alerts` | Cross-loan alert workflow hub | Advanced filters, grouped list, bulk state transitions | `GET /alerts` (new), `PATCH /alerts/{id}/status` |
| `/app/reports` | Export center (MVP) | Loan picker, dataset selector, date range, export action, local history | Existing export endpoints |
| `/app/admin/users` | User administration | User table, create user drawer, roles edit, deactivate confirm | `GET/POST /users`, `PATCH /users/{id}/roles`, `DELETE /users/{id}` |
| `/app/settings` | User preferences (MVP) | Profile summary, notification toggles, display prefs | `GET /auth/me` (new), local preference persistence |

## Navigation and Flow Rules
1. Global app shell includes persistent left nav, top contextual bar, breadcrumb, and role-aware menu pruning.
2. Dashboard quick actions deep-link into Loans, Alert Center, and Reports with pre-applied filters.
3. Portfolio widgets route to `/app/loans?risk=...` and `/app/alerts?status=...`.
4. Loans table row click routes to `/app/loans/:id/overview`; tabs switch as nested routes.
5. Alert Center row click routes to `/app/loans/:id/alerts?focusAlert=:alertId`.
6. Reports page consumes context from current loan when entered from loan detail.
7. Forbidden routes render RBAC-safe `403` page and never flash hidden content.

## Public API and Interface Additions Needed
### Required Backend Additions (for full page parity)
1. `GET /api/v1/loans/{loanId}/covenants` returning covenant list.
2. `GET /api/v1/loans/{loanId}/financial-statements` with pagination + sort.
3. `GET /api/v1/alerts` cross-loan pageable list with filters: `loanId,status,severity,type,from,to,q`.
4. `GET /api/v1/auth/me` returning `username,email,roles,active`.

### Operational MVP Extensions (Reports/Settings)
1. Reports v1 uses existing export endpoints and client-side history.
2. Settings v1 stores display/notification preferences client-side and uses `/auth/me` for identity.
3. Report templates, scheduled exports, API token CRUD, and server-side settings are phase-2 contracts.

### Frontend Type and Contract Changes
1. Replace current narrow enums in `frontend/src/types/api.ts` with full backend enum surface.
2. Add `AuthSession`, `UserRole`, `AlertStatus`, `RiskDetails`, `PortfolioSummary`, `ActivityLog`, `AttachmentMetadata`, `BulkImportSummary`, `UserResponse`.
3. Introduce unified `ProblemDetails` interface for RFC7807 + `correlationId`.
4. Define route param contract types for all nested loan tabs.

## Frontend Architecture Conventions (Feb 2026-aligned)
1. Route-first modular architecture with feature folders and lazy-loaded route chunks.
2. Centralized server-state cache/invalidation and optimistic mutation for alert status updates.
3. OpenAPI-driven typed client generation with runtime schema guards for critical payloads.
4. Token lifecycle handling with refresh flow, retry guard, and silent logout on refresh failure.
5. Role policy map used by both router guards and component-level action guards.
6. Accessibility baseline: WCAG 2.2 AA, keyboard-complete navigation, reduced-motion support, semantic tables/forms.
7. UX resilience: skeleton loading, empty states, inline validation, recoverable error banners with correlation ID.
8. Desktop-first density with responsive breakpoints for tablet/mobile read workflows.
9. Theming architecture uses semantic CSS variables with dark default and light token parity from day one.
10. Brand styling applies primarily in shell/navigation while data surfaces remain calm and legible.
11. Motion stays purposeful: page-load orchestration and state transitions, avoiding excessive decorative animation.

## Delivery Plan (Core-First)
### Phase 0: Frontend Foundation
1. Auth migration to JWT/refresh, secure storage strategy, API interceptors.
2. App shell, route tree, role guards, not-found/forbidden states.
3. Shared primitives: table, filters, forms, status badges, confirm modal, pagination.

### Phase 1: Core Operations
1. Loans list page.
2. Loan detail tabs: Overview, Statements, Results, Alerts.
3. Alert Center cross-loan workflow.
4. Dashboard rewrite for real role-specific operational summary.

### Phase 2: Oversight and Governance
1. Portfolio Overview.
2. Loan detail tabs: Documents, Comments, Activity.
3. Reports (operational MVP).
4. Settings (operational MVP).

### Phase 3: Administration
1. User Management page.
2. Hardening pass for RBAC edge cases and admin workflows.

### Phase 4: Design Theming and Polish Gate
1. Produce 2-3 visual theme directions on the same IA.
2. Use the locked direction in this plan as baseline and validate via 1 high-fidelity dashboard and 1 data-heavy table screen.
3. Finalize component-level tokens (tables, cards, badges, tabs, modals, forms, charts) for both dark and light variables.
4. Apply consistent motion, typography, and chart language across all pages.

## Testing and Acceptance
### Functional Scenarios
1. Analyst full flow: login -> create loan -> add covenant -> submit statement -> acknowledge alert -> export results.
2. Risk Lead flow: login -> portfolio review -> move alert to under-review/resolved with notes -> verify activity log.
3. Admin flow: login -> create user -> update roles -> deactivate user.
4. Loan detail document/comment/activity lifecycle end-to-end.
5. Reports export from both loan detail and global reports page.
6. Settings persistence across sessions.

### RBAC Scenarios
1. Analyst blocked from portfolio and user management.
2. Risk Lead blocked from create loan/covenant/statement actions.
3. Role-restricted alert transitions enforce backend and UI consistency.

### Quality Gates
1. Unit tests for guards, permission matrix, mappers, validators.
2. Integration tests with mocked API for each page route.
3. E2E smoke suite for each role and key flow.
4. Accessibility checks (keyboard traversal, labels, contrast, focus order).
5. Performance checks for first route load and large-table interactions.

## Assumptions and Defaults
1. Backend remains Spring Boot SPA host; `/app/**` routing remains valid.
2. Current stack is retained (React + Vite + Tailwind + Recharts).
3. Reports and Settings ship as operational MVP in first release.
4. API gaps listed above are part of delivery scope, not deferred.
5. No implementation starts until design direction/theme is reviewed and locked.
6. Dark mode is launch default; light mode tokens are planned and maintained in parity from the start.
