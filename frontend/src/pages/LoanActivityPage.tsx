import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getLoanActivity } from "../api/client";
import type { ActivityLog } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";

export function LoanActivityPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [rows, setRows] = useState<ActivityLog[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const page = await getLoanActivity(numericLoanId);
        setRows(page.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [numericLoanId]);

  return (
    <Surface className="p-4">
      <h2 className="panel-title">Loan Activity Trail</h2>
      <table className="table-base mt-3">
        <thead>
          <tr>
            <th>Time</th>
            <th>Event</th>
            <th>Actor</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.timestampUtc}</td>
              <td><Badge>{row.eventType}</Badge></td>
              <td>{row.username}</td>
              <td>{row.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
