import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Area, AreaChart, Line, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { getAlertsForLoan, getCovenantResults, getLoans, getRiskSummary } from "../api/client";
import type { Alert, CovenantResult, Loan, RiskSummary } from "../types/api";
import { PageSection, StatCard } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";

export function DashboardPage() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | null>(null);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [summary, setSummary] = useState<RiskSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

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
        const [resultPage, alertPage, riskSummary] = await Promise.all([
          getCovenantResults(selectedLoanId),
          getAlertsForLoan(selectedLoanId),
          getRiskSummary(selectedLoanId),
        ]);
        setResults(resultPage.content);
        setAlerts(alertPage.content);
        setSummary(riskSummary);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [selectedLoanId]);

  const chartData = useMemo(
    () =>
      [...results]
        .reverse()
        .slice(-10)
        .map((item, idx) => ({ index: idx + 1, value: Number(item.actualValue) })),
    [results]
  );
  const activeTab = "today";

  return (
    <PageSection
      title="Operational Dashboard"
      subtitle="Role-specific daily cockpit with alerts, results, and loan quick switching."
      action={
        <Select
          className="w-72"
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
      <Tabs value={activeTab} onValueChange={() => {}}>
        <TabsList className="mb-3">
          <TabsTrigger value="today">Today</TabsTrigger>
          <TabsTrigger value="week">7D</TabsTrigger>
          <TabsTrigger value="month">30D</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Loans Loaded" value={loans.length} />
        <StatCard label="Active Alerts" value={summary?.activeWarnings ?? 0} tone="MEDIUM" />
        <StatCard label="Breaches" value={summary?.breachedCount ?? 0} tone="HIGH" />
        <StatCard label="Risk Level" value={summary?.overallRiskLevel ?? "-"} tone={summary?.overallRiskLevel} />
      </div>

      <div className="mt-3 grid gap-3 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader className="border-b border-[var(--border-default)] pb-3">
            <div className="flex items-center justify-between">
              <CardTitle>Covenant Evaluation Trend</CardTitle>
              <Badge className="font-numeric">{chartData.length} points</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-3">
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="trendFade" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="0%" stopColor="#ffffff" stopOpacity={0.24} />
                      <stop offset="100%" stopColor="#ffffff" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="index" hide />
                  <Tooltip
                    cursor={{ stroke: "#3d3d3d" }}
                    contentStyle={{
                      background: "#0f0f0f",
                      border: "1px solid #2a2a2a",
                      borderRadius: "2px",
                      color: "#fff",
                    }}
                  />
                  <Area type="monotone" dataKey="value" stroke="none" fill="url(#trendFade)" />
                  <Line
                    type="monotone"
                    dataKey="value"
                    stroke="#f5f7fa"
                    strokeWidth={2}
                    dot={{ r: 2.2, fill: "#f5f7fa", stroke: "#060606", strokeWidth: 1 }}
                    activeDot={{ r: 4, fill: "#ffffff", stroke: "#060606", strokeWidth: 2 }}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-3">
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent className="pt-3">
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

      <div className="mt-3 grid gap-3 xl:grid-cols-2">
        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-3">
            <CardTitle>Recent Results</CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {results.slice(0, 6).map((result) => (
                  <TableRow key={result.id}>
                    <TableCell className="font-numeric">{result.id}</TableCell>
                    <TableCell>{result.covenantType}</TableCell>
                    <TableCell>
                      <Badge>{result.status}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b border-[var(--border-default)] pb-3">
            <div className="flex items-center justify-between">
              <CardTitle>My Open Alerts</CardTitle>
              <Badge className="font-numeric">{alerts.length}</Badge>
            </div>
          </CardHeader>
          <CardContent className="pt-2">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Severity</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {alerts.slice(0, 6).map((alert) => (
                  <TableRow key={alert.id}>
                    <TableCell className="font-numeric">{alert.id}</TableCell>
                    <TableCell>
                      <Badge>{alert.severityLevel}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge>{alert.status}</Badge>
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
