import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { UserRole } from "../types/api";
import { formatEnumLabel } from "../lib/format";
import { BrandLogo } from "./BrandLogo";

type MenuItem = {
  label: string;
  to: string;
  roles: UserRole[];
  icon:
    | "dashboard"
    | "portfolio"
    | "loans"
    | "alerts"
    | "integrations"
    | "workflows"
    | "policies"
    | "change-control"
    | "reports"
    | "users"
    | "loan-imports"
    | "settings";
};

const MENU: MenuItem[] = [
  { label: "Dashboard", to: "/app/dashboard", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "dashboard" },
  { label: "Portfolio", to: "/app/portfolio", roles: ["RISK_LEAD", "ADMIN"], icon: "portfolio" },
  { label: "Loans", to: "/app/loans", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "loans" },
  { label: "Alerts", to: "/app/alerts", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "alerts" },
  { label: "Integrations", to: "/app/integrations", roles: ["ADMIN"], icon: "integrations" },
  { label: "Workflows", to: "/app/workflows", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "workflows" },
  { label: "Policy Studio", to: "/app/policies", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "policies" },
  { label: "Change Control", to: "/app/change-control", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "change-control" },
  { label: "Reports", to: "/app/reports", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "reports" },
  { label: "Users", to: "/app/admin/users", roles: ["ADMIN"], icon: "users" },
  { label: "Loan Imports", to: "/app/admin/loan-imports", roles: ["ADMIN"], icon: "loan-imports" },
  { label: "Settings", to: "/app/settings", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "settings" },
];

export function AppShell() {
  const { userName, roles, logout } = useAuth();
  const visibleItems = MENU.filter((item) => item.roles.some((role) => roles.includes(role)));

  return (
    <div className="min-h-screen md:grid md:grid-cols-[88px_1fr] xl:grid-cols-[260px_1fr]">
      <aside className="relative border-b border-[var(--border-default)] bg-[var(--bg-surface-1)] p-3 md:min-h-screen md:border-b-0 md:border-r md:p-4">
        <div className="absolute inset-y-0 left-0 hidden w-[2px] bg-gradient-to-b from-transparent via-[var(--accent-primary)] to-transparent xl:block" />

        <div className="mb-5 rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3 xl:mb-7 xl:p-4">
          <BrandLogo size="sm" withText={false} className="justify-center xl:justify-start" />
          <p className="mt-3 hidden text-base font-semibold tracking-[-0.01em] text-[var(--text-primary)] xl:block">CovenantIQ</p>
          <p className="mt-1 hidden text-xs text-[var(--text-secondary)] xl:block">
            Secure covenant risk monitoring workspace
          </p>
        </div>

        <nav className="grid grid-cols-3 gap-2 md:grid-cols-1 md:gap-1.5">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              data-testid={`nav-${item.label.toLowerCase().replace(/\s+/g, "-")}`}
              className={({ isActive }) =>
                `group flex items-center justify-center gap-2 rounded-md border px-3 py-2 text-sm font-semibold transition-colors duration-150 md:justify-center xl:justify-start ${
                  isActive
                    ? "border-[var(--accent-primary)] bg-[var(--accent-soft)] text-[var(--text-primary)]"
                    : "border-transparent text-[var(--text-secondary)] hover:border-[var(--border-default)] hover:bg-[var(--bg-surface-2)] hover:text-[var(--text-primary)]"
                }`
              }
            >
              <span className="grid h-7 w-7 place-items-center rounded-md border border-[var(--border-default)] bg-[var(--bg-surface-3)] text-[var(--text-secondary)] group-hover:text-[var(--text-primary)]">
                <NavIcon kind={item.icon} />
              </span>
              <span className="hidden xl:inline">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-10 h-14 border-b border-[var(--border-default)] bg-[var(--bg-surface-1)] px-4 md:px-5">
          <div className="flex h-full items-center justify-end gap-3">
            <div className="flex items-center gap-3">
              <p className="hidden text-xs text-[var(--text-secondary)] md:block">
                {userName} | {roles.map((role) => formatEnumLabel(role)).join(", ")}
              </p>
              <button type="button" className="btn-secondary" onClick={logout} data-testid="logout-button">
                Logout
              </button>
            </div>
          </div>
        </header>

        <main className="p-4 md:p-5 xl:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function NavIcon({ kind }: { kind: MenuItem["icon"] }) {
  if (kind === "dashboard") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M2 2h5v5H2V2Zm7 0h5v3H9V2ZM9 7h5v7H9V7ZM2 9h5v5H2V9Z" fill="currentColor" /></svg>;
  }
  if (kind === "portfolio") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M2 12.5h12M3 12V8.5m3 3.5V5.5m3 6.5V7m3 5V3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>;
  }
  if (kind === "loans") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><rect x="2.5" y="3" width="11" height="10" rx="1" stroke="currentColor" strokeWidth="1.3"/><path d="M5 6h6M5 9h6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>;
  }
  if (kind === "alerts") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M8 2.3 14 13H2L8 2.3Z" stroke="currentColor" strokeWidth="1.3"/><path d="M8 6v3.2M8 11.4h.01" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>;
  }
  if (kind === "reports") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M3 2.5h7l3 3V13.5H3V2.5Z" stroke="currentColor" strokeWidth="1.3"/><path d="M10 2.5v3h3" stroke="currentColor" strokeWidth="1.3"/></svg>;
  }
  if (kind === "integrations") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M3.5 5.2a2.2 2.2 0 1 0 0 4.4m9-4.4a2.2 2.2 0 1 1 0 4.4M5.7 7.4h4.6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>;
  }
  if (kind === "workflows") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><rect x="2.7" y="2.7" width="3.6" height="3.6" rx=".8" stroke="currentColor" strokeWidth="1.2"/><rect x="9.7" y="9.7" width="3.6" height="3.6" rx=".8" stroke="currentColor" strokeWidth="1.2"/><path d="M6.4 4.5h3.2v3.2" stroke="currentColor" strokeWidth="1.2"/><path d="M9.6 7.7 6.2 11" stroke="currentColor" strokeWidth="1.2"/></svg>;
  }
  if (kind === "policies") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M3 3h10v10H3z" stroke="currentColor" strokeWidth="1.2"/><path d="M5 6.2h6M5 8.4h6M5 10.6h3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/></svg>;
  }
  if (kind === "change-control") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M8 2.5v2.1M8 11.4v2.1M3.8 4.2l1.5 1.5M10.7 11.1l1.5 1.5M2.5 8h2.1M11.4 8h2.1M3.8 11.8l1.5-1.5M10.7 4.9l1.5-1.5" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/><circle cx="8" cy="8" r="2.1" stroke="currentColor" strokeWidth="1.2"/></svg>;
  }
  if (kind === "users") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><circle cx="6" cy="6" r="2.3" stroke="currentColor" strokeWidth="1.3"/><path d="M2.8 12.5c.4-1.6 1.7-2.7 3.2-2.7s2.8 1.1 3.2 2.7" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/><path d="M12.5 6.2v3.6M10.7 8h3.6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>;
  }
  if (kind === "loan-imports") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M3 2.8h6l4 4v6.4H3V2.8Z" stroke="currentColor" strokeWidth="1.2"/><path d="M9 2.8v4h4" stroke="currentColor" strokeWidth="1.2"/><path d="M5.2 10.2h5.6M5.2 12.2h5.6M8 8.1V12.2" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/></svg>;
  }
  return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M8 2.5a1.5 1.5 0 0 1 1.5 1.5v.2a4.4 4.4 0 0 1 1.1.6l.2-.2a1.5 1.5 0 1 1 2.1 2.1l-.2.2c.3.3.5.7.6 1.1h.2a1.5 1.5 0 1 1 0 3h-.2a4.4 4.4 0 0 1-.6 1.1l.2.2a1.5 1.5 0 0 1-2.1 2.1l-.2-.2a4.4 4.4 0 0 1-1.1.6v.2a1.5 1.5 0 1 1-3 0v-.2a4.4 4.4 0 0 1-1.1-.6l-.2.2a1.5 1.5 0 0 1-2.1-2.1l.2-.2a4.4 4.4 0 0 1-.6-1.1h-.2a1.5 1.5 0 0 1 0-3h.2a4.4 4.4 0 0 1 .6-1.1l-.2-.2A1.5 1.5 0 0 1 5.2 4.6l.2.2c.3-.3.7-.5 1.1-.6V4A1.5 1.5 0 0 1 8 2.5Zm0 4A2 2 0 1 0 8 10.5 2 2 0 0 0 8 6.5Z" stroke="currentColor" strokeWidth="1"/></svg>;
}
