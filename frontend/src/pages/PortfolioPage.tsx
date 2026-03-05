import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { getPortfolioSummary } from "../api/client";
import { PageSection, StatCard, Surface } from "../components/layout";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";
import { chartTokenValue } from "../theme/theme";
import type { PortfolioSummary } from "../types/api";

export function PortfolioPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
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

  const trendData = useMemo(
    () => (
      sampleUxEnabled
        ? [
            { month: "Sep", high: 6, medium: 11, low: 14 },
            { month: "Oct", high: 7, medium: 10, low: 15 },
            { month: "Nov", high: 5, medium: 12, low: 16 },
            { month: "Dec", high: 6, medium: 11, low: 15 },
            { month: "Jan", high: 4, medium: 13, low: 17 },
            {
              month: "Feb",
              high: summary?.highRiskLoanCount ?? 0,
              medium: summary?.mediumRiskLoanCount ?? 0,
              low: summary?.lowRiskLoanCount ?? 0,
            },
          ]
        : []
    ),
    [sampleUxEnabled, summary?.highRiskLoanCount, summary?.lowRiskLoanCount, summary?.mediumRiskLoanCount]
  );

  return (
    <PageSection title="Portfolio Oversight" subtitle="Risk distribution and alert heat across active portfolio.">
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Active Loans" value={summary?.totalActiveLoans ?? 0} />
        <StatCard label="Breaches" value={summary?.totalBreaches ?? 0} tone="HIGH" />
        <StatCard label="Open Alerts" value={summary?.totalOpenAlerts ?? 0} tone="MEDIUM" />
        <StatCard label="Under Review" value={summary?.totalUnderReviewAlerts ?? 0} />
      </div>

      <div className="mt-3 grid gap-3 xl:grid-cols-3">
        <Surface className="xl:col-span-2 p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="panel-title">Risk Trend</h2>
            {!sampleUxEnabled ? <span className="text-xs text-[var(--text-secondary)]">Sample history disabled</span> : null}
          </div>
          <div className="mt-2 h-64">
            {trendData.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trendData}>
                  <defs>
                    <linearGradient id="riskFade" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="0%" stopColor={chartTokenValue.accent} stopOpacity={0.35} />
                      <stop offset="100%" stopColor={chartTokenValue.accent} stopOpacity={0.03} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="month" stroke={chartTokenValue.neutral1} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="high"
                    stackId="risk"
                    stroke={chartTokenValue.high}
                    fill="url(#riskFade)"
                    fillOpacity={1}
                  />
                  <Area
                    type="monotone"
                    dataKey="medium"
                    stackId="risk"
                    stroke={chartTokenValue.medium}
                    fill={chartTokenValue.medium}
                    fillOpacity={0.14}
                  />
                  <Area
                    type="monotone"
                    dataKey="low"
                    stackId="risk"
                    stroke={chartTokenValue.low}
                    fill={chartTokenValue.low}
                    fillOpacity={0.1}
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="grid h-full place-items-center rounded-md border border-dashed border-[var(--border-default)] text-sm text-[var(--text-secondary)]">
                Historical trend data is not synthesized in normal mode.
              </div>
            )}
          </div>
        </Surface>

        <Surface className="p-5">
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
