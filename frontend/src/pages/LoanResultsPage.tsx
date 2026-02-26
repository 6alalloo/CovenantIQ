import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { getCovenantResults } from "../api/client";
import type { CovenantResult, CovenantResultStatus } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";

export function LoanResultsPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [status, setStatus] = useState<"ALL" | CovenantResultStatus>("ALL");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const page = await getCovenantResults(numericLoanId, 0, 200);
        setResults(page.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [numericLoanId]);

  const filtered = useMemo(
    () => results.filter((item) => (status === "ALL" ? true : item.status === status)),
    [results, status]
  );

  return (
    <Surface className="p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="panel-title">Covenant Evaluation Timeline</h2>
        <select className="input w-56" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}>
          <option value="ALL">All statuses</option>
          <option value="PASS">PASS</option>
          <option value="BREACH">BREACH</option>
        </select>
      </div>
      <table className="table-base">
        <thead>
          <tr>
            <th>ID</th>
            <th>Covenant</th>
            <th>Actual</th>
            <th>Status</th>
            <th>Timestamp</th>
          </tr>
        </thead>
        <tbody>
          {filtered.map((row) => (
            <tr key={row.id}>
              <td className="font-numeric">{row.id}</td>
              <td>{row.covenantType}</td>
              <td className="font-numeric">{row.actualValue}</td>
              <td><Badge>{row.status}</Badge></td>
              <td>{row.evaluationTimestampUtc}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
