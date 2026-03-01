# Frontend Visual Redesign Plan (Maybe + Actual + Invoice Ninja, Dark-First)

## Summary
Redesign CovenantIQ's frontend to a modern, professional, low-bloat fintech style by combining:
1. Maybe-led polish for KPI and overview surfaces.
2. Actual-style clarity for dense financial workflows and report tiles.
3. Invoice Ninja operational structure for admin and workflow pages.

Locked preferences:
1. Style mix: Maybe-led.
2. Density: Balanced.
3. Theme scope: Dark-first with light-token parity.
4. Layout: Persistent left rail.
5. Accent usage: Moderate navy/blue accents.
6. Charts: Monochrome + semantic risk colors.
7. Rollout: Big-bang redesign.

## Reference Visual Dive (What We Borrow)
### Maybe
1. Portfolio-first dashboard posture with high-value summaries and account context.
2. Dark-mode-ready presentation with calm, premium cards.
3. Applied to dashboard hero section, KPI hierarchy, and overview polish.

### Actual Budget
1. Clear sidebar-led layout behavior.
2. Practical table-first workflows and tiled reports.
3. Applied to table ergonomics, data legibility, and report modules.

### Invoice Ninja
1. Operational dashboard framing and status-heavy workflow modules.
2. Explicit mode controls (`system`, `light`, `dark`) validating tokenized theming.
3. Applied to alerts, settings, and workflow-oriented arrangement.

## Implemented Visual System
### 1) Theme Tokens
Implemented semantic tokens in `frontend/src/styles/index.css` with:
1. `:root` dark defaults.
2. `:root[data-theme="dark"]` behavior.
3. `:root[data-theme="light"]` parity tokens.

Dark tokens include:
`--bg-app`, `--bg-surface-1..3`, `--border-default`, `--border-strong`, `--text-primary`, `--text-secondary`, `--text-muted`, `--accent-primary`, `--accent-primary-hover`, `--accent-soft`, `--focus-ring`, `--risk-high`, `--risk-medium`, `--risk-low`, `--chart-neutral-1`, `--chart-neutral-2`, `--chart-accent`.

Light parity tokens are implemented and available through `data-theme="light"`.

### 2) Typography
Implemented:
1. UI/body: `Manrope`.
2. Numeric/tabular: `IBM Plex Mono`.
3. Heading/body weighting aligned to professional enterprise tone.

### 3) Spacing, Radius, Density
Implemented targets:
1. Balanced spacing and less visual bloat.
2. Control height standardized to `40px`.
3. Radius standardized (`6px` controls, `8px` card surfaces).
4. Border + tonal separation over heavy shadows.

### 4) Layout Arrangement
Implemented shell:
1. Persistent left rail.
2. Medium-width icon rail and expanded desktop rail.
3. Sticky top context bar with route context and user actions.
4. Refined page gutters and content spacing.

### 5) Component Styling Rules
Implemented across UI primitives:
1. `Card`: matte dark surfaces, restrained border, modern spacing.
2. `Button`: primary navy, secondary neutral, ghost minimal.
3. `Tabs`: soft active background and border-forward active states.
4. `Table`: improved hierarchy, sticky header behavior in component primitives.
5. `Badge/Chip`: semantic low-opacity statuses.
6. `Input/Select`: neutral fills with visible focus ring.
7. `Charts`: grayscale baseline + navy accent with semantic risk colors.

### 6) Motion
Implemented:
1. Minimal functional transitions.
2. Page enter adjusted to ~200ms.
3. Existing reduced-motion behavior preserved.

## Implementation Blueprint (Delivered)
### Added Files
1. `frontend/src/theme/theme.ts`
2. `frontend/src/theme/ThemeProvider.tsx`
3. `frontend/src/theme/useTheme.ts`
4. `docs/frontend_visual_redesign_plan.md`

### Modified Files
1. `frontend/src/main.tsx` (theme provider wiring)
2. `frontend/src/styles/index.css` (tokenized dark/light system + component styling refresh)
3. `frontend/src/components/AppShell.tsx`
4. `frontend/src/components/layout.tsx`
5. `frontend/src/components/BrandLogo.tsx`
6. `frontend/src/components/ui/button.tsx`
7. `frontend/src/components/ui/card.tsx`
8. `frontend/src/components/ui/select.tsx`
9. `frontend/src/components/ui/tabs.tsx`
10. `frontend/src/components/ui/table.tsx`
11. `frontend/src/components/ui/badge.tsx`
12. `frontend/src/components/ui/switch.tsx`
13. `frontend/src/pages/DashboardPage.tsx`
14. `frontend/src/pages/LoansPage.tsx`
15. `frontend/src/pages/LoanDetailLayoutPage.tsx`
16. `frontend/src/pages/LoanOverviewPage.tsx`
17. `frontend/src/pages/LoanResultsPage.tsx`
18. `frontend/src/pages/LoanStatementsPage.tsx`
19. `frontend/src/pages/LoanAlertsPage.tsx`
20. `frontend/src/pages/LoanDocumentsPage.tsx`
21. `frontend/src/pages/LoanCommentsPage.tsx`
22. `frontend/src/pages/LoanActivityPage.tsx`
23. `frontend/src/pages/AlertsPage.tsx`
24. `frontend/src/pages/ReportsPage.tsx`
25. `frontend/src/pages/SettingsPage.tsx`
26. `frontend/src/pages/LoginPage.tsx`
27. `frontend/src/pages/PortfolioPage.tsx`
28. `frontend/src/pages/AdminUsersPage.tsx`
29. `frontend/src/pages/ForbiddenPage.tsx`
30. `frontend/src/pages/NotFoundPage.tsx`

## Public APIs / Types
Backend API changes:
1. None required.

Frontend additions:
1. `ThemeMode = "dark" | "light" | "system"`.
2. Theme context interface (`mode`, `resolvedMode`, `setMode`).
3. Theme storage key `covenantiq.theme_mode`.
4. Chart token mapping for consistent palette usage.

## Validation Targets
1. Build compiles with zero type errors.
2. Existing route map and RBAC behavior unchanged.
3. Dark mode is default and persisted.
4. `system` theme resolves dynamically from OS preference.
5. Light token parity renders without style breakage.
6. Key workflows remain legible and balanced at desktop and tablet widths.
