import { useEffect, useState } from "react";
import { NavLink, Outlet, useParams } from "react-router-dom";
import { getLoan } from "../api/client";
import { PageSection } from "../components/layout";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";
import type { Loan } from "../types/api";

const TABS = [
  { key: "overview", label: "Overview", icon: "[]"},
  { key: "statements", label: "Statements", icon: "Fx"},
  { key: "results", label: "Results", icon: "~"},
  { key: "alerts", label: "Alerts", icon: "!"},
  { key: "collateral", label: "Collateral", icon: "$"},
  { key: "documents", label: "Documents", icon: "D"},
  { key: "comments", label: "Comments", icon: "C"},
  { key: "activity", label: "Activity", icon: "A"},
];

export function LoanDetailLayoutPage() {
  const { loanId } = useParams();
  const pathKey = window.location.pathname.split("/").pop() ?? "overview";
  const numericLoanId = Number(loanId);
  const [loan, setLoan] = useState<Loan | null>(null);

  useEffect(() => {
    if (!Number.isFinite(numericLoanId)) return;
    (async () => {
      try {
        const data = await getLoan(numericLoanId);
        setLoan(data);
      } catch {
        setLoan(null);
      }
    })();
  }, [numericLoanId]);

  const loanTitle = loan?.borrowerName ? loan.borrowerName : `Loan #${loanId}`;

  return (
    <PageSection
      title={loanTitle}
      subtitle="Nested operations for covenant setup, statements, alerts, and collaboration."
    >
      <Tabs value={pathKey} onValueChange={() => {}}>
        <TabsList className="mb-1">
          {TABS.map((tab) => (
            <NavLink key={tab.key} to={`/app/loans/${loanId}/${tab.key}`}>
              {({ isActive }) => (
                <TabsTrigger
                  value={tab.key}
                  data-testid={`loan-tab-${tab.key}`}
                  className={isActive ? "border-[var(--accent-primary)] bg-[var(--accent-soft)] text-[var(--text-primary)]" : ""}
                >
                  <span className="font-numeric text-[11px]">{tab.icon}</span>
                  {tab.label}
                </TabsTrigger>
              )}
            </NavLink>
          ))}
        </TabsList>
      </Tabs>
      <Outlet />
    </PageSection>
  );
}
