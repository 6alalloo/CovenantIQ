import { useEffect, useState } from "react";
import { useSearchParams, useParams } from "react-router-dom";
import { getAlertsForLoan, updateAlertStatus } from "../api/client";
import type { Alert, AlertStatus } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { formatAlertMessage, formatEnumLabel } from "../lib/format";

export function LoanAlertsPage() {
  const { loanId } = useParams();
  const [searchParams] = useSearchParams();
  const focusAlert = searchParams.get("focusAlert");
  const numericLoanId = Number(loanId);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const page = await getAlertsForLoan(numericLoanId, 0, 100);
      setAlerts(page.content);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [numericLoanId]);

  const transition = async (alertId: number, status: AlertStatus) => {
    try {
      await updateAlertStatus(alertId, status, status === "RESOLVED" ? "Resolved in loan workflow." : undefined);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Surface className="p-5">
      <h2 className="panel-title">Loan Alert Operations</h2>
      <table className="table-base mt-3" data-testid="loan-alerts-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Message</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {alerts.map((alert) => (
            <tr
              key={alert.id}
              data-testid={`alert-row-${alert.id}`}
              className={focusAlert && Number(focusAlert) === alert.id ? "bg-[var(--accent-soft)]" : ""}
            >
              <td className="font-numeric">{alert.id}</td>
              <td>{formatEnumLabel(alert.alertType)}</td>
              <td><Badge>{formatEnumLabel(alert.severityLevel)}</Badge></td>
              <td data-testid={`alert-status-${alert.id}`}><Badge>{formatEnumLabel(alert.status)}</Badge></td>
              <td>{formatAlertMessage(alert.message)}</td>
              <td>
                <div className="flex gap-2">
                  {alert.status === "OPEN" ? (
                    <Button
                      variant="outline"
                      data-testid={`alert-ack-${alert.id}`}
                      onClick={() => void transition(alert.id, "ACKNOWLEDGED")}
                    >
                      Acknowledge
                    </Button>
                  ) : null}
                  {(alert.status === "OPEN" || alert.status === "ACKNOWLEDGED") && (
                    <Button
                      variant="outline"
                      data-testid={`alert-review-${alert.id}`}
                      onClick={() => void transition(alert.id, "UNDER_REVIEW")}
                    >
                      Review
                    </Button>
                  )}
                  {alert.status !== "RESOLVED" ? (
                    <Button
                      variant="outline"
                      data-testid={`alert-resolve-${alert.id}`}
                      onClick={() => void transition(alert.id, "RESOLVED")}
                    >
                      Resolve
                    </Button>
                  ) : null}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
