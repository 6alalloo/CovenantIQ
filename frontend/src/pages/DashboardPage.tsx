import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Area, AreaChart, Line, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { getAlertsForLoan, getCovenantResults, getLoans } from "../api/client";
import type { Alert, CovenantResult, Loan, RiskLevel } from "../types/api";
import { PageSection, StatCard } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";
import { formatEnumLabel } from "../lib/format";
import { chartTokenValue } from "../theme/theme";

export function DashboardPage() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | null>(null);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [range, setRange] = useState<"today" | "30d" | "90d" | "365d" | "all">("today");

  useEffect(() => {
    (async () => {
      try {
        const page = await getLoans();
        setLoans(page.content);
        if (page.content.length) {
          setSelectedLoanId(page.content[0].id);
        }
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, []);

  useEffect(() => {
    if (!selectedLoanId) return;
    (async () => {
      try {
        const [resultPage, alertPage] = await Promise.all([
          getCovenantResults(selectedLoanId, 0, 200),
          getAlertsForLoan(selectedLoanId, 0, 200),
        ]);
        setResults(resultPage.content);
        setAlerts(alertPage.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [selectedLoanId]);

  const chartData = useMemo(
    () =>
      [...filterByRange(results, range, (item) => item.evaluationTimestampUtc)]
        .reverse()
        .slice(-10)
        .map((item, idx) => ({ index: idx + 1, value: Number(item.actualValue) })),
    [range, results]
  );

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

  return (
    <PageSection
      title="Operational Dashboard"
      subtitle="Role-specific cockpit for covenant trajectory, alert pressure, and rapid workflow handoff."
      action={
        <Select
          className="w-full max-w-[320px]"
          value={selectedLoanId ?? ""}
          onChange={(event) => setSelectedLoanId(Number(event.target.value))}
        >
          {loans.map((loan) => (
            <option key={loan.id} value={loan.id}>
              #{loan.id} {loan.borrowerName}
            </option>
          ))}
        </Select>
      }
    >
      <Tabs value={range} onValueChange={(value) => setRange(value as typeof range)}>
        <TabsList>
          <TabsTrigger value="today">Today</TabsTrigger>
          <TabsTrigger value="30d">30D</TabsTrigger>
          <TabsTrigger value="90d">90D</TabsTrigger>
          <TabsTrigger value="365d">365D</TabsTrigger>
          <TabsTrigger value="all">All Time</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Loans Loaded" value={loans.length} />
        <StatCard label="Active Alerts" value={activeWarnings} tone="MEDIUM" />
        <StatCard label="Breaches" value={breachedCount} tone="HIGH" />
        <StatCard label="Risk Level" value={formatEnumLabel(riskLevel)} tone={riskLevel} />
      </div>

      <div className="grid gap-3 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <div className="flex items-center justify-between">
              <CardTitle>Covenant Trajectory</CardTitle>
              <Badge className="font-numeric">{chartData.length} points</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-4">
            <div className="h-[290px]">
              {chartData.length ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <defs>
                      <linearGradient id="trendFade" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0%" stopColor={chartTokenValue.accent} stopOpacity={0.32} />
                        <stop offset="100%" stopColor={chartTokenValue.accent} stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="index" hide />
                    <Tooltip
                      cursor={{ stroke: "var(--border-strong)" }}
                      contentStyle={{
                        background: "var(--bg-surface-2)",
                        border: "1px solid var(--border-default)",
                        borderRadius: "8px",
                        color: "var(--text-primary)",
                      }}
                    />
                    <Area type="monotone" dataKey="value" stroke={chartTokenValue.neutral2} strokeWidth={1.25} fill="url(#trendFade)" />
                    <Line
                      type="monotone"
                      dataKey="value"
                      stroke={chartTokenValue.accent}
                      strokeWidth={2}
                      dot={{ r: 2.4, fill: chartTokenValue.accent, stroke: "var(--bg-app)", strokeWidth: 1 }}
                      activeDot={{ r: 4, fill: chartTokenValue.accent, stroke: "var(--bg-app)", strokeWidth: 2 }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="grid h-full place-items-center text-sm text-[var(--text-secondary)]">
                  No covenant evaluations in this time range.
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-4">
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent className="pt-4">
            <div className="grid gap-2">
              <Link to="/app/loans">
                <Button variant="outline" className="w-full justify-between">
                  Open Loan Directory
                  <span className="font-numeric text-xs">01</span>
                </Button>
              </Link>
              <Link to="/app/alerts">
                <Button variant="outline" className="w-full justify-between">
                  Open Alert Center
                  <span className="font-numeric text-xs">02</span>
                </Button>
              </Link>
              <Link to="/app/reports">
                <Button variant="outline" className="w-full justify-between">
                  Open Reports
                  <span className="font-numeric text-xs">03</span>
                </Button>
              </Link>
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
                  <TableHead>ID</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredResults.slice(0, 6).map((result) => (
                  <TableRow key={result.id}>
                    <TableCell className="font-numeric">{result.id}</TableCell>
                    <TableCell>{formatEnumLabel(result.covenantType)}</TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(result.status)}</Badge>
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
                  <TableHead>ID</TableHead>
                  <TableHead>Severity</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredAlerts.slice(0, 6).map((alert) => (
                  <TableRow key={alert.id}>
                    <TableCell className="font-numeric">{alert.id}</TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(alert.severityLevel)}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge>{formatEnumLabel(alert.status)}</Badge>
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
