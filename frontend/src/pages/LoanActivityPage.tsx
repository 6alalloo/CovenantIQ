import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getLoan, getLoanActivity } from "../api/client";
import type { ActivityLog, Loan } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { formatDateTime, formatEnumLabel } from "../lib/format";

export function LoanActivityPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [rows, setRows] = useState<ActivityLog[]>([]);
  const [loan, setLoan] = useState<Loan | null>(null);
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

  const formatDescription = (description: string) => {
    if (!loan?.borrowerName) return description;
    const pattern = new RegExp(`loan\\s*#?\\s*${numericLoanId}\\b`, "gi");
    return description.replace(pattern, loan.borrowerName);
  };

  return (
    <Surface className="p-5">
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
              <td>{formatDateTime(row.timestampUtc)}</td>
              <td><Badge>{formatEnumLabel(row.eventType)}</Badge></td>
              <td>{row.username}</td>
              <td>{formatDescription(row.description)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
