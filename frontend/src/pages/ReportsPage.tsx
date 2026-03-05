import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { exportLoanAlerts, exportLoanCovenantResults, getLoans } from "../api/client";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";
import { formatDateTime, formatEnumLabel } from "../lib/format";
import type { Loan } from "../types/api";

type ExportHistory = {
  id: number;
  timestamp: string;
  loanId: number;
  dataset: "alerts" | "covenant-results";
  from: string;
  to: string;
};

export function ReportsPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const [searchParams] = useSearchParams();
  const seedLoan = searchParams.get("loanId");
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | "">("");
  const [dataset, setDataset] = useState<"alerts" | "covenant-results">("alerts");
  const [from, setFrom] = useState(sampleUxEnabled ? "2026-01-01" : "");
  const [to, setTo] = useState(sampleUxEnabled ? "2026-02-26" : "");
  const [history, setHistory] = useState<ExportHistory[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    if (!sampleUxEnabled) {
      setFrom("");
      setTo("");
    }
  }, [sampleUxEnabled]);

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

  const onExport = async () => {
    if (!selectedLoanId) return;
    setError(null);
    setIsExporting(true);
    try {
      const response =
        dataset === "alerts"
          ? await exportLoanAlerts(selectedLoanId)
          : await exportLoanCovenantResults(selectedLoanId);

      const blob = await response.blob();
      const disposition = response.headers.get("content-disposition") ?? "";
      const match = disposition.match(/filename="?([^"]+)"?/i);
      const fallback = `${dataset}-${selectedLoanId}-${new Date().toISOString().slice(0, 10)}.csv`;
      const filename = match?.[1] ?? fallback;

      const downloadUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = downloadUrl;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(downloadUrl);

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
    } catch (e) {
      setError(`Export failed: ${(e as Error).message}`);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <PageSection title="Reports & Export" subtitle="Operational export center with client-side history for the current browser session.">
      <div className="grid gap-3 xl:grid-cols-[1fr_1.2fr]">
        <Surface className="p-5">
          <h2 className="panel-title">Export Setup</h2>
          <div className="mt-3 space-y-3">
            <Select
              value={selectedLoanId}
              onChange={(event) => setSelectedLoanId(Number(event.target.value))}
            >
              <option value="">Select a loan</option>
              {loans.map((loan) => (
                <option key={loan.id} value={loan.id}>
                  #{loan.id} {loan.borrowerName}
                </option>
              ))}
            </Select>

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

            <Button className="w-full" onClick={() => void onExport()} disabled={!selectedLoanId || isExporting} type="button">
              {isExporting ? "Exporting..." : "Export"}
            </Button>
          </div>
        </Surface>

        <Surface className="p-5">
          <h2 className="panel-title">Local Export History</h2>
          <Table className="mt-2">
            <TableHeader>
              <TableRow>
                <TableHead>When</TableHead>
                <TableHead>Loan</TableHead>
                <TableHead>Dataset</TableHead>
                <TableHead>Range</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {history.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>{formatDateTime(item.timestamp)}</TableCell>
                  <TableCell className="font-numeric">#{item.loanId}</TableCell>
                  <TableCell>{formatEnumLabel(item.dataset)}</TableCell>
                  <TableCell>
                    {item.from || "-"} to {item.to || "-"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Surface>
      </div>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
