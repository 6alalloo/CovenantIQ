import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { exportLoanAlerts, exportLoanCovenantResults, getLoans } from "../api/client";
import type { Loan } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

type ExportHistory = {
  id: number;
  timestamp: string;
  loanId: number;
  dataset: "alerts" | "covenant-results";
  from: string;
  to: string;
};

export function ReportsPage() {
  const [searchParams] = useSearchParams();
  const seedLoan = searchParams.get("loanId");
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | "">("");
  const [dataset, setDataset] = useState<"alerts" | "covenant-results">("alerts");
  const [from, setFrom] = useState("2026-01-01");
  const [to, setTo] = useState("2026-02-26");
  const [history, setHistory] = useState<ExportHistory[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const page = await getLoans();
        setLoans(page.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, []);

  useEffect(() => {
    if (seedLoan) {
      setSelectedLoanId(Number(seedLoan));
    }
  }, [seedLoan]);

  const url = useMemo(() => {
    if (!selectedLoanId) return "#";
    return dataset === "alerts" ? exportLoanAlerts(selectedLoanId) : exportLoanCovenantResults(selectedLoanId);
  }, [dataset, selectedLoanId]);

  const onExport = () => {
    if (!selectedLoanId) return;
    setHistory((previous) => [
      {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        loanId: selectedLoanId,
        dataset,
        from,
        to,
      },
      ...previous,
    ]);
    window.open(url, "_blank", "noopener,noreferrer");
  };

  return (
    <PageSection title="Reports & Export" subtitle="Operational export center with client-side history for MVP.">
      <div className="grid gap-3 xl:grid-cols-[1fr_1.2fr]">
        <Surface className="p-4">
          <h2 className="panel-title">Export Setup</h2>
          <div className="mt-3 space-y-3">
            <select
              className="input"
              value={selectedLoanId}
              onChange={(event) => setSelectedLoanId(Number(event.target.value))}
            >
              <option value="">Select a loan</option>
              {loans.map((loan) => (
                <option key={loan.id} value={loan.id}>
                  #{loan.id} {loan.borrowerName}
                </option>
              ))}
            </select>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                className="seg-btn justify-center"
                data-active={dataset === "alerts"}
                onClick={() => setDataset("alerts")}
              >
                <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M8 2.3 14 13H2L8 2.3Z" stroke="currentColor" strokeWidth="1.3"/><path d="M8 6v3.2M8 11.4h.01" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/></svg>
                Alerts CSV
              </button>
              <button
                type="button"
                className="seg-btn justify-center"
                data-active={dataset === "covenant-results"}
                onClick={() => setDataset("covenant-results")}
              >
                <svg width="13" height="13" viewBox="0 0 16 16" fill="none"><path d="M3 12.5h12M3 12V8.5m3 3.5V5.5m3 6.5V7m3 5V3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>
                Results CSV
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
              <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
            </div>

            <Button className="w-full" onClick={onExport} disabled={!selectedLoanId} type="button">
              Export
            </Button>
          </div>
        </Surface>

        <Surface className="p-4">
          <h2 className="panel-title">Local Export History</h2>
          <table className="table-base mt-2">
            <thead>
              <tr>
                <th>When</th>
                <th>Loan</th>
                <th>Dataset</th>
                <th>Range</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{item.timestamp}</td>
                  <td className="font-numeric">#{item.loanId}</td>
                  <td>{item.dataset}</td>
                  <td>
                    {item.from} to {item.to}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Surface>
      </div>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
