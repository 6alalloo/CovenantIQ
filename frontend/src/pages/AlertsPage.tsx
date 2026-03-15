import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getAlertsGlobal, getLoans, updateAlertStatus } from "../api/client";
import type { Alert, AlertStatus } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { formatAlertMessage, formatEnumLabel } from "../lib/format";

const STATUS_COLORS: Record<string, string> = {
  ACKNOWLEDGED: "#3B82F6",
  OPEN: "#F59E0B",
  UNDER_REVIEW: "#8B5CF6",
  RESOLVED: "#10B981",
};

const SEVERITY_COLORS: Record<Alert["severityLevel"], string> = {
  LOW: "#38BDF8",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
};

function severityBadgeClass(severityLevel: Alert["severityLevel"]) {
  return severityLevel === "HIGH"
    ? "border-[color:rgb(239_68_68_/_0.28)] bg-[rgb(239_68_68_/_0.15)] text-[#EF4444]"
    : severityLevel === "MEDIUM"
      ? "border-[color:rgb(245_158_11_/_0.28)] bg-[rgb(245_158_11_/_0.15)] text-[#F59E0B]"
      : "border-[color:rgb(56_189_248_/_0.28)] bg-[rgb(56_189_248_/_0.15)] text-[#38BDF8]";
}

function statusBadgeClass(status: Alert["status"]) {
  return status === "ACKNOWLEDGED"
    ? "border-[color:rgb(59_130_246_/_0.28)] bg-[rgb(59_130_246_/_0.14)] text-[#3B82F6]"
    : status === "OPEN"
      ? "border-[color:rgb(245_158_11_/_0.28)] bg-[rgb(245_158_11_/_0.14)] text-[#F59E0B]"
      : status === "UNDER_REVIEW"
        ? "border-[color:rgb(139_92_246_/_0.28)] bg-[rgb(139_92_246_/_0.14)] text-[#8B5CF6]"
        : "border-[color:rgb(16_185_129_/_0.28)] bg-[rgb(16_185_129_/_0.14)] text-[#10B981]";
}

function SeverityBadge({ severityLevel }: { severityLevel: Alert["severityLevel"] }) {
  const color = SEVERITY_COLORS[severityLevel];
  return (
    <span
      className={`chip gap-1.5 px-2.5 py-1 ${severityBadgeClass(severityLevel)}`}
      style={{ borderColor: `${color}47`, backgroundColor: `${color}26`, color }}
    >
      <span className="inline-block h-1.5 w-1.5 rounded-full" style={{ backgroundColor: color }} />
      {formatEnumLabel(severityLevel)}
    </span>
  );
}

function StatusBadge({ status }: { status: Alert["status"] }) {
  const color = STATUS_COLORS[status];
  return (
    <span
      className={`chip px-2.5 py-1 ${statusBadgeClass(status)}`}
      style={{ borderColor: `${color}47`, backgroundColor: `${color}24`, color }}
    >
      {formatEnumLabel(status)}
    </span>
  );
}

function ChartLegend({
  items,
}: {
  items: Array<{ label: string; color: string }>;
}) {
  return (
    <div className="mt-3 flex flex-wrap gap-2">
      {items.map((item) => (
        <span key={item.label} className="chip gap-1.5 px-2.5 py-1" style={{ borderColor: `${item.color}33`, color: "var(--text-secondary)" }}>
          <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: item.color }} />
          {item.label}
        </span>
      ))}
    </div>
  );
}

export function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [borrowersByLoan, setBorrowersByLoan] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const statusFilter = searchParams.get("status") ?? "ALL";

  const load = async () => {
    try {
      const [rows, loanPage] = await Promise.all([getAlertsGlobal(), getLoans(0, 200)]);
      setAlerts(rows.sort((a, b) => b.id - a.id));
      setBorrowersByLoan(
        loanPage.content.reduce<Record<number, string>>((acc, loan) => {
          acc[loan.id] = loan.borrowerName;
          return acc;
        }, {})
      );
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const filtered = useMemo(
    () => alerts.filter((alert) => (statusFilter === "ALL" ? true : alert.status === statusFilter)),
    [alerts, statusFilter]
  );

  const bulkTransition = async (status: AlertStatus) => {
    const candidates = filtered.slice(0, 5);
    try {
      await Promise.all(candidates.map((item) => updateAlertStatus(item.id, status)));
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const statusData = useMemo(() => {
    const counts = new Map<string, number>();
    filtered.forEach((item) => counts.set(item.status, (counts.get(item.status) ?? 0) + 1));
    return [...counts.entries()].map(([key, value]) => ({ key, label: formatEnumLabel(key), value }));
  }, [filtered]);

  const severityData = useMemo(() => {
    const counts = new Map<string, number>();
    filtered.forEach((item) => counts.set(item.severityLevel, (counts.get(item.severityLevel) ?? 0) + 1));
    return [...counts.entries()].map(([key, value]) => ({ key, label: formatEnumLabel(key), value }));
  }, [filtered]);

  return (
    <PageSection
      title="Alert Center"
      subtitle="Cross-loan alert workflow hub with filters and batch transitions."
      action={
        <Select
          className="w-52"
          value={statusFilter}
          onChange={(event) => setSearchParams({ status: event.target.value })}
        >
          <option value="ALL">All statuses</option>
          <option value="OPEN">{formatEnumLabel("OPEN")}</option>
          <option value="ACKNOWLEDGED">{formatEnumLabel("ACKNOWLEDGED")}</option>
          <option value="UNDER_REVIEW">{formatEnumLabel("UNDER_REVIEW")}</option>
          <option value="RESOLVED">{formatEnumLabel("RESOLVED")}</option>
        </Select>
      }
    >
      <div className="grid gap-3 xl:grid-cols-2">
        <Surface className="p-5">
          <h2 className="panel-title">Status Distribution</h2>
          <div className="mt-3 h-48">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusData}>
                <XAxis dataKey="label" tick={{ fill: "var(--text-secondary)", fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fill: "var(--text-secondary)", fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                  {statusData.map((entry) => (
                    <Cell key={entry.key} fill={STATUS_COLORS[entry.key] ?? STATUS_COLORS.RESOLVED} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <ChartLegend
            items={[
              { label: "Acknowledged", color: STATUS_COLORS.ACKNOWLEDGED },
              { label: "Open", color: STATUS_COLORS.OPEN },
              { label: "Under Review", color: STATUS_COLORS.UNDER_REVIEW },
              { label: "Resolved", color: STATUS_COLORS.RESOLVED },
            ]}
          />
        </Surface>

        <Surface className="p-5">
          <h2 className="panel-title">Severity Distribution</h2>
          <div className="mt-3 h-48">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={severityData}
                  dataKey="value"
                  nameKey="label"
                  innerRadius={45}
                  outerRadius={72}
                  paddingAngle={2}
                  stroke="var(--bg-surface-1)"
                  strokeWidth={2}
                >
                  {severityData.map((entry) => (
                    <Cell key={entry.key} fill={SEVERITY_COLORS[entry.key as Alert["severityLevel"]] ?? SEVERITY_COLORS.LOW} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ChartLegend
            items={[
              { label: "High", color: SEVERITY_COLORS.HIGH },
              { label: "Medium", color: SEVERITY_COLORS.MEDIUM },
              { label: "Low", color: SEVERITY_COLORS.LOW },
            ]}
          />
        </Surface>
      </div>

      <Surface className="p-5">
        <div className="mb-3 flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => void bulkTransition("UNDER_REVIEW")}>
            Move Top 5 to Under Review
          </Button>
          <Button variant="outline" onClick={() => void bulkTransition("RESOLVED")}>
            Resolve Top 5
          </Button>
          <p className="self-center text-sm text-[var(--text-secondary)]">Showing {filtered.length} alerts</p>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Alert</TableHead>
              <TableHead>Borrower</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Severity</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Message</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((alert) => (
              <TableRow key={alert.id}>
                <TableCell className="font-numeric">{alert.id}</TableCell>
                <TableCell>{borrowersByLoan[alert.loanId] ?? `Loan #${alert.loanId}`}</TableCell>
                <TableCell>{formatEnumLabel(alert.alertType)}</TableCell>
                <TableCell><SeverityBadge severityLevel={alert.severityLevel} /></TableCell>
                <TableCell><StatusBadge status={alert.status} /></TableCell>
                <TableCell>{formatAlertMessage(alert.message)}</TableCell>
                <TableCell className="text-right">
                  <Link className="btn-secondary" to={`/app/loans/${alert.loanId}/alerts?focusAlert=${alert.id}`}>
                    View Loan
                  </Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Surface>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
