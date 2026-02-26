import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { UserRole } from "../types/api";
import { BrandLogo } from "./BrandLogo";

type MenuItem = {
  label: string;
  to: string;
  roles: UserRole[];
  icon: "dashboard" | "portfolio" | "loans" | "alerts" | "reports" | "users" | "settings";
};

const MENU: MenuItem[] = [
  { label: "Dashboard", to: "/app/dashboard", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "dashboard" },
  { label: "Portfolio", to: "/app/portfolio", roles: ["RISK_LEAD", "ADMIN"], icon: "portfolio" },
  { label: "Loans", to: "/app/loans", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "loans" },
  { label: "Alerts", to: "/app/alerts", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "alerts" },
  { label: "Reports", to: "/app/reports", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "reports" },
  { label: "Users", to: "/app/admin/users", roles: ["ADMIN"], icon: "users" },
  { label: "Settings", to: "/app/settings", roles: ["ANALYST", "RISK_LEAD", "ADMIN"], icon: "settings" },
];

export function AppShell() {
  const { userName, roles, logout } = useAuth();
  const location = useLocation();
  const visibleItems = MENU.filter((item) => item.roles.some((role) => roles.includes(role)));

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[258px_1fr]">
      <aside className="relative border-r border-[var(--border-default)] bg-[var(--bg-surface-1)] p-4">
        <div className="absolute inset-y-0 left-0 w-[2px] bg-gradient-to-b from-transparent via-[var(--accent-interactive)] to-transparent" />

        <div className="mb-7 rounded-sm border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-4">
          <BrandLogo size="sm" />
          <p className="mt-3 text-xs text-[var(--text-secondary)]">Secure covenant risk monitoring workspace</p>
        </div>

        <nav className="space-y-1.5">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `group flex items-center gap-2 rounded-sm border px-3 py-2 text-sm transition ${
                  isActive
                    ? "border-[color:rgb(142_184_255_/_0.45)] bg-[color:rgb(142_184_255_/_0.12)] text-[var(--text-primary)]"
                    : "border-transparent text-[var(--text-secondary)] hover:border-[var(--border-default)] hover:bg-[color:rgb(255_255_255_/_0.02)] hover:text-[var(--text-primary)]"
                }`
              }
            >
              <span className="grid h-6 w-6 place-items-center rounded-sm border border-[var(--border-default)] bg-black/70 text-[var(--text-secondary)] group-hover:text-[var(--text-primary)]">
                <NavIcon kind={item.icon} />
              </span>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-10 border-b border-[var(--border-default)] bg-[color:rgb(6_6_6_/_0.9)] px-5 py-3 backdrop-blur">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[10px] uppercase tracking-[0.16em] text-[var(--text-secondary)]">Active Route</p>
              <p className="font-numeric text-xs text-[var(--accent-interactive)]">{location.pathname}</p>
            </div>
            <div className="flex items-center gap-3">
              <p className="text-xs text-[var(--text-secondary)]">
                {userName} | {roles.join(", ")}
              </p>
              <button type="button" className="btn-secondary" onClick={logout}>
                Logout
              </button>
            </div>
          </div>
        </header>

        <main className="p-5 md:p-6">
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
  if (kind === "users") {
    return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><circle cx="6" cy="6" r="2.3" stroke="currentColor" strokeWidth="1.3"/><path d="M2.8 12.5c.4-1.6 1.7-2.7 3.2-2.7s2.8 1.1 3.2 2.7" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/><path d="M12.5 6.2v3.6M10.7 8h3.6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>;
  }
  return <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M8 2.5a1.5 1.5 0 0 1 1.5 1.5v.2a4.4 4.4 0 0 1 1.1.6l.2-.2a1.5 1.5 0 1 1 2.1 2.1l-.2.2c.3.3.5.7.6 1.1h.2a1.5 1.5 0 1 1 0 3h-.2a4.4 4.4 0 0 1-.6 1.1l.2.2a1.5 1.5 0 0 1-2.1 2.1l-.2-.2a4.4 4.4 0 0 1-1.1.6v.2a1.5 1.5 0 1 1-3 0v-.2a4.4 4.4 0 0 1-1.1-.6l-.2.2a1.5 1.5 0 0 1-2.1-2.1l.2-.2a4.4 4.4 0 0 1-.6-1.1h-.2a1.5 1.5 0 0 1 0-3h.2a4.4 4.4 0 0 1 .6-1.1l-.2-.2A1.5 1.5 0 0 1 5.2 4.6l.2.2c.3-.3.7-.5 1.1-.6V4A1.5 1.5 0 0 1 8 2.5Zm0 4A2 2 0 1 0 8 10.5 2 2 0 0 0 8 6.5Z" stroke="currentColor" strokeWidth="1"/></svg>;
}
