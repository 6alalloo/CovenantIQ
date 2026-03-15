import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getAlertsForLoan,
  getAlertsGlobal,
  getCovenantResults,
  getCovenantResultsGlobal,
  getLoans,
  getPortfolioSummary,
} from "../api/client";
import type { Alert, CovenantResult, Loan, PortfolioSummary, RiskLevel } from "../types/api";
import { PageSection } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";
import { formatAlertMessage, formatDateTime, formatEnumLabel, formatNumber } from "../lib/format";

export function DashboardPage() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | null>(null);
  const [results, setResults] = useState<Array<CovenantResult & { loanId: number }>>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [portfolioSummary, setPortfolioSummary] = useState<PortfolioSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [range, setRange] = useState<"today" | "30d" | "90d" | "365d" | "all">("today");

  useEffect(() => {
    (async () => {
      try {
        const page = await getLoans();
        setLoans(page.content);
        setSelectedLoanId(null);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const summary = await getPortfolioSummary();
        setPortfolioSummary(summary);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      try {
        if (!selectedLoanId) {
          const [globalResults, globalAlerts] = await Promise.all([getCovenantResultsGlobal(), getAlertsGlobal()]);
          setResults(globalResults);
          setAlerts(globalAlerts);
          return;
        }

        const [resultPage, alertPage] = await Promise.all([
          getCovenantResults(selectedLoanId, 0, 200),
          getAlertsForLoan(selectedLoanId, 0, 200),
        ]);
        setResults(resultPage.content.map((item) => ({ ...item, loanId: selectedLoanId })));
        setAlerts(alertPage.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [selectedLoanId]);

  const filteredResults = useMemo(
    () => filterByRange(results, range, (item) => item.evaluationTimestampUtc),
    [range, results]
  );
  const filteredAlerts = useMemo(
    () => filterByRange(alerts, range, (item) => item.triggeredTimestampUtc),
    [alerts, range]
  );

  const activeWarnings = filteredAlerts.filter((item) => item.status !== "RESOLVED").length;
  const breachedCount = filteredResults.filter((item) => item.status === "BREACH").length;
  const riskLevel: RiskLevel = breachedCount > 0 ? "HIGH" : activeWarnings > 0 ? "MEDIUM" : "LOW";
  const borrowersByLoan = useMemo(
    () =>
      loans.reduce<Record<number, string>>((acc, loan) => {
        acc[loan.id] = loan.borrowerName;
        return acc;
      }, {}),
    [loans]
  );

  const riskMix = useMemo(() => {
    if (selectedLoanId) {
      return {
        totalActiveLoans: 1,
        highRiskLoanCount: riskLevel === "HIGH" ? 1 : 0,
        mediumRiskLoanCount: riskLevel === "MEDIUM" ? 1 : 0,
        lowRiskLoanCount: riskLevel === "LOW" ? 1 : 0,
        totalBreaches: breachedCount,
      };
    }
    if (portfolioSummary) {
      return {
        totalActiveLoans: portfolioSummary.totalActiveLoans,
        highRiskLoanCount: portfolioSummary.highRiskLoanCount,
        mediumRiskLoanCount: portfolioSummary.mediumRiskLoanCount,
        lowRiskLoanCount: portfolioSummary.lowRiskLoanCount,
        totalBreaches: portfolioSummary.totalBreaches,
      };
    }
    return null;
  }, [breachedCount, portfolioSummary, riskLevel, selectedLoanId]);

  const alertPipeline = useMemo(() => {
    if (selectedLoanId) {
      const open = filteredAlerts.filter((item) => item.status === "OPEN").length;
      const underReview = filteredAlerts.filter((item) => item.status === "UNDER_REVIEW").length;
      return {
        open,
        underReview,
        breaches: breachedCount,
      };
    }
    if (portfolioSummary) {
      return {
        open: portfolioSummary.totalOpenAlerts,
        underReview: portfolioSummary.totalUnderReviewAlerts,
        breaches: portfolioSummary.totalBreaches,
      };
    }
    return null;
  }, [breachedCount, filteredAlerts, portfolioSummary, selectedLoanId]);

  return (
    <PageSection
      title="Operational Dashboard"
      subtitle="Role-specific cockpit for covenant trajectory, alert pressure, and rapid workflow handoff."
    >
      <Card className="dashboard-hero">
        <CardHeader className="border-b border-[var(--border-default)] pb-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="dashboard-eyebrow">Portfolio Overview</p>
              <CardTitle className="mt-2 text-xl">Covenant Control Center</CardTitle>
              <p className="mt-2 text-sm text-[var(--text-secondary)]">
                Track covenant performance, risk exposure, and alert velocity for the selected facility.
              </p>
            </div>
            <div className="flex w-full flex-wrap items-center gap-3 sm:w-auto">
              <Select
                className="w-full min-w-[220px] max-w-[320px]"
                value={selectedLoanId ?? ""}
                onChange={(event) => {
                  const value = event.target.value;
                  setSelectedLoanId(value ? Number(value) : null);
                }}
              >
                <option value="">All loans</option>
                {loans.map((loan) => (
                  <option key={loan.id} value={loan.id}>
                    #{loan.id} {loan.borrowerName}
                  </option>
                ))}
              </Select>
              <Tabs value={range} onValueChange={(value) => setRange(value as typeof range)}>
                <TabsList>
                  <TabsTrigger value="today">Today</TabsTrigger>
                  <TabsTrigger value="30d">30D</TabsTrigger>
                  <TabsTrigger value="90d">90D</TabsTrigger>
                  <TabsTrigger value="365d">365D</TabsTrigger>
                  <TabsTrigger value="all">All Time</TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-5">
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <div className="dashboard-kpi">
              <p className="dashboard-kpi__label">Loans Loaded</p>
              <p className="dashboard-kpi__value">{loans.length}</p>
              <p className="dashboard-kpi__meta">Active facilities in scope</p>
            </div>
            <div className="dashboard-kpi">
              <p className="dashboard-kpi__label">Active Alerts</p>
              <p className="dashboard-kpi__value" data-risk="MEDIUM">
                {activeWarnings}
              </p>
              <p className="dashboard-kpi__meta">Open or acknowledged</p>
            </div>
            <div className="dashboard-kpi">
              <p className="dashboard-kpi__label">Breaches</p>
              <p className="dashboard-kpi__value" data-risk="HIGH">
                {breachedCount}
              </p>
              <p className="dashboard-kpi__meta">Triggered covenant failures</p>
            </div>
            <div className="dashboard-kpi">
              <p className="dashboard-kpi__label">Risk Level</p>
              <p className="dashboard-kpi__value" data-risk={riskLevel}>
                {formatEnumLabel(riskLevel)}
              </p>
              <p className="dashboard-kpi__meta">Weighted by breach volume</p>
            </div>
          </div>

          <div className="mt-4 grid gap-3 lg:grid-cols-3">
            <div className="dashboard-rail lg:col-span-2">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="dashboard-eyebrow">Alert Pressure</p>
                  <p className="mt-2 text-sm text-[var(--text-secondary)]">
                    Monitoring cadence across the selected range.
                  </p>
                </div>
                <Badge className="font-numeric">{filteredAlerts.length} signals</Badge>
              </div>
              <div className="mt-4 grid gap-2 sm:grid-cols-2">
                <div className="dashboard-rail__item">
                  Active alerts
                  <span className="font-numeric text-[var(--text-primary)]">{activeWarnings}</span>
                </div>
                <div className="dashboard-rail__item">
                  Breach events
                  <span className="font-numeric text-[var(--text-primary)]">{breachedCount}</span>
                </div>
                <div className="dashboard-rail__item">
                  Range points
                  <span className="font-numeric text-[var(--text-primary)]">{filteredResults.length}</span>
                </div>
                <div className="dashboard-rail__item">
                  Current posture
                  <span className="font-numeric" data-risk={riskLevel}>
                    {formatEnumLabel(riskLevel)}
                  </span>
                </div>
              </div>
            </div>

            <div className="dashboard-rail">
              <p className="dashboard-eyebrow">Action Dock</p>
              <div className="mt-4 grid gap-2">
                <Link to="/app/loans" className="dashboard-action">
                  Open Loan Directory
                  <span className="font-numeric text-xs text-[var(--text-muted)]">01</span>
                </Link>
                <Link to="/app/alerts" className="dashboard-action">
                  Open Alert Center
                  <span className="font-numeric text-xs text-[var(--text-muted)]">02</span>
                </Link>
                <Link to="/app/reports" className="dashboard-action">
                  Open Reports
                  <span className="font-numeric text-xs text-[var(--text-muted)]">03</span>
                </Link>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-3 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <div className="flex items-center justify-between">
              <CardTitle>Portfolio Risk Mix</CardTitle>
              <Badge className="font-numeric">{riskMix?.totalActiveLoans ?? 0} active loans</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-4">
            {riskMix ? (
              <div className="space-y-5">
                <div>
                  <div className="flex items-center justify-between text-sm text-[var(--text-secondary)]">
                    <span>Risk distribution</span>
                    <span className="font-numeric">{riskMix.totalActiveLoans} total</span>
                  </div>
                  <div className="mt-3 h-3 w-full overflow-hidden rounded-full border border-[var(--border-default)] bg-[var(--bg-surface-3)]">
                    <div className="flex h-full w-full">
                      {(() => {
                        const total = Math.max(riskMix.totalActiveLoans, 1);
                        const highPct = (riskMix.highRiskLoanCount / total) * 100;
                        const medPct = (riskMix.mediumRiskLoanCount / total) * 100;
                        const lowPct = (riskMix.lowRiskLoanCount / total) * 100;
                        return (
                          <>
                            <div className="h-full bg-[var(--risk-high)]" style={{ width: `${highPct}%` }} />
                            <div className="h-full bg-[var(--risk-medium)]" style={{ width: `${medPct}%` }} />
                            <div className="h-full bg-[var(--risk-low)]" style={{ width: `${lowPct}%` }} />
                          </>
                        );
                      })()}
                    </div>
                  </div>
                  <div className="mt-3 grid gap-3 sm:grid-cols-3">
                    <div className="dashboard-rail__item">
                      High risk
                      <span className="font-numeric" data-risk="HIGH">
                        {riskMix.highRiskLoanCount}
                      </span>
                    </div>
                    <div className="dashboard-rail__item">
                      Medium risk
                      <span className="font-numeric" data-risk="MEDIUM">
                        {riskMix.mediumRiskLoanCount}
                      </span>
                    </div>
                    <div className="dashboard-rail__item">
                      Low risk
                      <span className="font-numeric" data-risk="LOW">
                        {riskMix.lowRiskLoanCount}
                      </span>
                    </div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between text-sm text-[var(--text-secondary)]">
                    <span>Alert pipeline</span>
                    <span className="font-numeric">{alertPipeline?.breaches ?? 0} breaches</span>
                  </div>
                  <div className="mt-3 grid gap-3 sm:grid-cols-3">
                    <div className="dashboard-rail__item">
                      Open alerts
                      <span className="font-numeric">{alertPipeline?.open ?? 0}</span>
                    </div>
                    <div className="dashboard-rail__item">
                      Under review
                      <span className="font-numeric">{alertPipeline?.underReview ?? 0}</span>
                    </div>
                    <div className="dashboard-rail__item">
                      Total breaches
                      <span className="font-numeric">{alertPipeline?.breaches ?? 0}</span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="grid h-[240px] place-items-center text-sm text-[var(--text-secondary)]">
                Loading portfolio summary...
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <div className="flex items-center justify-between">
              <CardTitle>Alert Stream</CardTitle>
              <Badge className="font-numeric">{filteredAlerts.length}</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-4">
            <div className="grid gap-2">
              {filteredAlerts.slice(0, 5).map((alert) => (
                <div key={alert.id} className="dashboard-rail__item">
                  <div>
                    <p className="text-sm">Alert #{alert.id}</p>
                    <p className="text-xs text-[var(--text-muted)]">{formatEnumLabel(alert.severityLevel)}</p>
                  </div>
                  <Badge>{formatEnumLabel(alert.status)}</Badge>
                </div>
              ))}
              {!filteredAlerts.length ? (
                <div className="grid place-items-center rounded-md border border-dashed border-[var(--border-default)] p-4 text-sm text-[var(--text-secondary)]">
                  No alerts in the selected range.
                </div>
              ) : null}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-3 xl:grid-cols-2">
        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <CardTitle>Recent Results</CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Result</TableHead>
                  <TableHead>Borrower</TableHead>
                  <TableHead>Covenant</TableHead>
                  <TableHead>Actual</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Evaluated</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredResults.slice(0, 6).map((result) => (
                  <TableRow key={result.id}>
                    <TableCell className="font-numeric">#{result.id}</TableCell>
                    <TableCell>{borrowersByLoan[result.loanId] ?? `Loan #${result.loanId}`}</TableCell>
                    <TableCell>{formatEnumLabel(result.covenantType)}</TableCell>
                    <TableCell className="font-numeric">{formatNumber(result.actualValue)}</TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(result.status)}</Badge>
                    </TableCell>
                    <TableCell>{formatDateTime(result.evaluationTimestampUtc)}</TableCell>
                    <TableCell className="text-right">
                      <Link className="btn-secondary" to={`/app/loans/${result.loanId}/results`}>
                        View
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <div className="flex items-center justify-between">
              <CardTitle>My Open Alerts</CardTitle>
              <Badge className="font-numeric">{filteredAlerts.length}</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Alert</TableHead>
                  <TableHead>Borrower</TableHead>
                  <TableHead>Severity</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Message</TableHead>
                  <TableHead>Triggered</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredAlerts
                  .filter((alert) => alert.status !== "RESOLVED")
                  .slice(0, 6)
                  .map((alert) => (
                  <TableRow key={alert.id}>
                    <TableCell className="font-numeric">#{alert.id}</TableCell>
                    <TableCell>{borrowersByLoan[alert.loanId] ?? `Loan #${alert.loanId}`}</TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(alert.severityLevel)}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(alert.status)}</Badge>
                    </TableCell>
                    <TableCell>{formatAlertMessage(alert.message)}</TableCell>
                    <TableCell>{formatDateTime(alert.triggeredTimestampUtc)}</TableCell>
                    <TableCell className="text-right">
                      <Link className="btn-secondary" to={`/app/loans/${alert.loanId}/alerts?focusAlert=${alert.id}`}>
                        View
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}

function filterByRange<T>(items: T[], range: "today" | "30d" | "90d" | "365d" | "all", getTimestamp: (item: T) => string) {
  if (range === "all") {
    return items;
  }

  const now = new Date();
  const start = new Date(now);

  if (range === "today") {
    start.setHours(0, 0, 0, 0);
  } else if (range === "30d") {
    start.setDate(start.getDate() - 29);
    start.setHours(0, 0, 0, 0);
  } else if (range === "90d") {
    start.setDate(start.getDate() - 89);
    start.setHours(0, 0, 0, 0);
  } else {
    start.setDate(start.getDate() - 364);
    start.setHours(0, 0, 0, 0);
  }

  return items.filter((item) => {
    const ts = new Date(getTimestamp(item));
    return !Number.isNaN(ts.getTime()) && ts >= start && ts <= now;
  });
}
