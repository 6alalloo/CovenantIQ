import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { getPortfolioSummary } from "../api/client";
import type { PortfolioSummary } from "../types/api";
import { PageSection, StatCard, Surface } from "../components/layout";

export function PortfolioPage() {
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setSummary(await getPortfolioSummary());
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, []);

  const trendData = [
    { month: "Sep", high: 6, medium: 11, low: 14 },
    { month: "Oct", high: 7, medium: 10, low: 15 },
    { month: "Nov", high: 5, medium: 12, low: 16 },
    { month: "Dec", high: 6, medium: 11, low: 15 },
    { month: "Jan", high: 4, medium: 13, low: 17 },
    { month: "Feb", high: summary?.highRiskLoanCount ?? 0, medium: summary?.mediumRiskLoanCount ?? 0, low: summary?.lowRiskLoanCount ?? 0 },
  ];

  return (
    <PageSection title="Portfolio Oversight" subtitle="Risk distribution and alert heat across active portfolio.">
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Active Loans" value={summary?.totalActiveLoans ?? 0} />
        <StatCard label="Breaches" value={summary?.totalBreaches ?? 0} tone="HIGH" />
        <StatCard label="Open Alerts" value={summary?.totalOpenAlerts ?? 0} tone="MEDIUM" />
        <StatCard label="Under Review" value={summary?.totalUnderReviewAlerts ?? 0} />
      </div>

      <div className="mt-3 grid gap-3 xl:grid-cols-3">
        <Surface className="xl:col-span-2 p-4">
          <h2 className="panel-title">Risk Trend</h2>
          <div className="mt-2 h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trendData}>
                <defs>
                  <linearGradient id="riskFade" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor="#ffffff" stopOpacity={0.24} />
                    <stop offset="100%" stopColor="#ffffff" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="month" stroke="#71767a" tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="high"
                  stackId="risk"
                  stroke="#ffffff"
                  fill="url(#riskFade)"
                  fillOpacity={1}
                />
                <Area type="monotone" dataKey="medium" stackId="risk" stroke="#c9cdd1" fill="#c9cdd1" fillOpacity={0.12} />
                <Area type="monotone" dataKey="low" stackId="risk" stroke="#a8adb3" fill="#a8adb3" fillOpacity={0.08} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Surface>

        <Surface className="p-4">
          <h2 className="panel-title">Drill-through</h2>
          <div className="mt-3 grid gap-2">
            <Link className="btn-secondary text-center" to="/app/loans?risk=HIGH">
              High-Risk Loans
            </Link>
            <Link className="btn-secondary text-center" to="/app/alerts?status=OPEN">
              Open Alerts
            </Link>
          </div>
        </Surface>
      </div>

      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
