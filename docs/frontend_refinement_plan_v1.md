# Frontend Refinement Plan v1

## Objective
Implement targeted UX and behavior refinements requested after the visual redesign, focusing on clarity, workflow speed, and fewer confusing UI elements.

## Scope (Requested Changes)
1. Remove the `Active Route` section from the app header.
2. Make dashboard `7D` and `30D` filters functional.
3. Replace raw enum-style labels (underscores, backend naming) with user-friendly labels.
4. Loans page:
   - Rename `Open` button to `View`.
   - Allow row click to navigate to loan detail.
5. Add confirmation modal before closing a loan.
6. Alerts page:
   - Address confusing `404 visible` count presentation.
   - Improve alert messaging clarity.
   - Replace `Loan #` with borrower names.
   - Add a fitting graph.
7. Fix CSV export authorization (`401 Unauthorized`) for reports.
8. Redesign settings page with stronger structure and usability.

## Current-State Findings
1. Header still renders route text in `AppShell`.
2. Dashboard range tabs only set state; no data filtering logic uses `range`.
3. Raw labels are printed directly from API enums on multiple pages (`COVENANT_TYPE`, alert statuses, etc.).
4. Loans page currently requires explicit action button for navigation.
5. Loan close action has no guard rail.
6. Alerts page count badge can render values like `404 visible`, which looks like an error code.
7. CSV export uses `window.open(url)` with no `Authorization` header, causing `401`.
8. Settings page is functional but visually flat and not segmented by intent.

## Implementation Plan

### 1) Header Cleanup
1. File: `frontend/src/components/AppShell.tsx`
2. Remove `useLocation` usage and the left-side `Active Route` block.
3. Keep top bar minimal: user context + logout only.

### 2) Functional Dashboard Time Filters
1. File: `frontend/src/pages/DashboardPage.tsx`
2. Implement `useMemo`-based filtering keyed by selected range:
   - `today`: records from current local day.
   - `7D`: last 7 days inclusive.
   - `30D`: last 30 days inclusive.
3. Use `evaluationTimestampUtc` as primary date key.
4. Apply filtered dataset to:
   - trend chart
   - recent results table
   - optional open-alert sublist (if date field supports meaningful filtering).
5. Add empty-state copy when selected range has no rows.

### 3) Friendly Label Formatting
1. Add formatting utility module (new file): `frontend/src/lib/format.ts`
2. Implement:
   - `formatEnumLabel(value: string): string`
   - optional domain formatters for high-value labels:
     - covenant type (`DEBT_TO_EQUITY` => `Debt to Equity`)
     - alert type/status/severity
3. Replace direct enum rendering in:
   - dashboard tables
   - loans/loan detail tabs
   - alerts pages
   - reports/settings labels where backend terms appear.

### 4) Loans Page Navigation and Actions
1. File: `frontend/src/pages/LoansPage.tsx`
2. Replace action text `Open` -> `View`.
3. Make each loan row clickable (navigate to `/app/loans/:id/overview`).
4. Preserve action buttons and stop row-click propagation on buttons.
5. Add hover/focus row affordance to communicate clickability.

### 5) Close Loan Confirmation Modal
1. Add reusable modal component (new): `frontend/src/components/ui/confirm-dialog.tsx`
2. Integrate into `LoansPage` close flow:
   - clicking `Close` opens modal.
   - modal displays borrower + loan id for confirmation context.
   - actions: `Cancel`, `Confirm Close`.
3. Only execute `closeLoan()` after confirmation.
4. Disable modal confirm while request is in flight; surface API error.

### 6) Alerts Page Refinement
1. Files:
   - `frontend/src/pages/AlertsPage.tsx`
   - `frontend/src/api/client.ts` (only if needed for extra data helpers)
2. Replace badge text:
   - from `404 visible` style to explicit helper text, e.g. `Showing 404 alerts`.
3. Replace `Loan #` column value:
   - map `loanId -> borrowerName` by joining with loans data loaded once.
4. Improve message clarity:
   - create friendly message adapter for common system phrases.
   - preserve raw message in tooltip or secondary text if needed.
5. Add compact graph at top:
   - recommended: status distribution bar chart (Open / Acknowledged / Under Review / Resolved).
   - secondary option: severity distribution donut.

### 7) CSV Export Auth Fix
1. Files:
   - `frontend/src/api/client.ts`
   - `frontend/src/pages/ReportsPage.tsx`
2. Replace `window.open(exportUrl)` with authenticated fetch download flow:
   - call export endpoint via `fetch`/request wrapper with bearer token.
   - receive `Blob`.
   - create object URL and trigger file download.
3. Parse `content-disposition` for filename fallback.
4. Show user-facing error toast/message on failure with clear next action.

### 8) Settings Page Redesign
1. File: `frontend/src/pages/SettingsPage.tsx`
2. Restructure into clear sections:
   - `Profile & Access`
   - `Appearance` (theme mode)
   - `Notifications`
   - `Accessibility` (reduced motion)
3. Use card/tile layout inspired by Maybe/Actual:
   - concise labels
   - descriptive helper copy
   - explicit save state/last updated stamp
4. Keep existing preference persistence behavior.

## API / Interface Changes
1. Backend API: no contract change required.
2. Frontend additions:
   - `format` utility functions.
   - reusable confirm dialog component.
   - optional report export helper returning `Blob`.

## Acceptance Criteria
1. Header no longer shows active route text.
2. Dashboard `Today/7D/30D` visibly changes data.
3. Enum values render in user-friendly text everywhere in scope.
4. Loans rows are navigable by click; action button shows `View`.
5. Close-loan modal confirms before mutation.
6. Alerts page:
   - no ambiguous `404 visible` chip
   - borrower names displayed
   - includes status/severity graph
   - messages are clearer.
7. CSV export succeeds with authenticated session and downloads file.
8. Settings page is reorganized into clear, modern sections.
9. `npm run build` passes.

## Testing Plan
1. Unit:
   - enum formatter utility.
   - date range filter logic.
2. Integration/manual:
   - row click and button propagation on loans table.
   - close confirmation flow.
   - alerts borrower-name mapping.
   - CSV export authenticated download.
   - settings persistence after redesign.
3. Regression:
   - role-based route behavior unchanged.
   - dark/light theme switching unaffected.

## Clarifications Needed Before Implementation
1. Friendly labels:
   - **Locked**: Apply globally across the app.
2. Dashboard filters:
   - **Locked**: `7D/30D` must affect KPI cards too.
3. Alerts graph preference:
   - **Locked**: Include both status distribution and severity distribution visuals.
4. Alert message rewrite:
   - **Locked**: Do not expose original backend text to users.
5. Settings redesign:
   - **Locked**: Explicit save with unsaved-change confirmation when navigating away.
6. Screenshot note:
   - **Received** and incorporated into naming cleanup scope.
