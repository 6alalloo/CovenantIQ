import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getAlertsGlobal, updateAlertStatus } from "../api/client";
import type { Alert, AlertStatus } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";

export function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const statusFilter = searchParams.get("status") ?? "ALL";

  const load = async () => {
    try {
      const rows = await getAlertsGlobal();
      setAlerts(rows.sort((a, b) => b.id - a.id));
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

  return (
    <PageSection
      title="Alert Center"
      subtitle="Cross-loan alert workflow hub with filters and batch transitions."
      action={
        <select
          className="input w-52"
          value={statusFilter}
          onChange={(event) => setSearchParams({ status: event.target.value })}
        >
          <option value="ALL">All statuses</option>
          <option value="OPEN">OPEN</option>
          <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
          <option value="UNDER_REVIEW">UNDER_REVIEW</option>
          <option value="RESOLVED">RESOLVED</option>
        </select>
      }
    >
      <Surface className="p-4">
        <div className="mb-3 flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => void bulkTransition("UNDER_REVIEW")}>
            Move Top 5 to Under Review
          </Button>
          <Button variant="outline" onClick={() => void bulkTransition("RESOLVED")}>
            Resolve Top 5
          </Button>
          <Badge>{filtered.length} visible</Badge>
        </div>
        <table className="table-base">
          <thead>
            <tr>
              <th>Alert</th>
              <th>Loan</th>
              <th>Type</th>
              <th>Severity</th>
              <th>Status</th>
              <th>Message</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {filtered.map((alert) => (
              <tr key={alert.id}>
                <td className="font-numeric">{alert.id}</td>
                <td className="font-numeric">#{alert.loanId}</td>
                <td>{alert.alertType}</td>
                <td><Badge>{alert.severityLevel}</Badge></td>
                <td><Badge>{alert.status}</Badge></td>
                <td>{alert.message}</td>
                <td className="text-right">
                  <Link className="btn-secondary" to={`/app/loans/${alert.loanId}/alerts?focusAlert=${alert.id}`}>
                    Open Loan
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Surface>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
