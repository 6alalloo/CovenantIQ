import { ChangeEvent, useEffect, useState } from "react";
import {
  getLoanImportRows,
  getLoanImports,
  previewLoanImport,
  runLoanImport,
} from "../api/client";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import type { LoanImportBatch, LoanImportPreviewResponse, LoanImportRow } from "../types/api";

export function AdminLoanImportsPage() {
  const [history, setHistory] = useState<LoanImportBatch[]>([]);
  const [selectedBatchId, setSelectedBatchId] = useState<number | null>(null);
  const [selectedRows, setSelectedRows] = useState<LoanImportRow[]>([]);
  const [preview, setPreview] = useState<LoanImportPreviewResponse | null>(null);
  const [selectedFileName, setSelectedFileName] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHistory = async () => {
    try {
      const next = await getLoanImports();
      setHistory(next);
      if (!selectedBatchId && next.length) {
        setSelectedBatchId(next[0].id);
      }
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void loadHistory();
  }, []);

  useEffect(() => {
    if (!selectedBatchId) {
      setSelectedRows([]);
      return;
    }
    void (async () => {
      try {
        const rows = await getLoanImportRows(selectedBatchId);
        setSelectedRows(rows);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [selectedBatchId]);

  const onFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setBusy(true);
    setError(null);
    setSelectedFileName(file.name);
    try {
      const result = await previewLoanImport(file);
      setPreview(result);
      setSelectedBatchId(result.batch.id);
      setSelectedRows(result.rows);
      await loadHistory();
    } catch (e) {
      setError((e as Error).message);
      setPreview(null);
    } finally {
      setBusy(false);
      event.target.value = "";
    }
  };

  const onRunImport = async () => {
    if (!preview) return;
    setBusy(true);
    setError(null);
    try {
      const result = await runLoanImport(preview.batch.id);
      setPreview({ batch: result.batch, rows: result.rows });
      setSelectedBatchId(result.batch.id);
      setSelectedRows(result.rows);
      await loadHistory();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const activeBatch = history.find((batch) => batch.id === selectedBatchId) ?? preview?.batch ?? null;
  const activeRows = preview?.batch.id === selectedBatchId ? preview.rows : selectedRows;

  return (
    <PageSection
      title="Loan Imports"
      subtitle="Admin-only CSV import flow for externally managed loan master data. Imported rows create or update loan shells only; monitoring configuration stays user-managed in CovenantIQ."
    >
      <div className="grid gap-4 xl:grid-cols-[1.25fr_0.95fr]">
        <div className="space-y-4">
          <Surface className="p-5">
            <h2 className="panel-title">Upload CSV</h2>
            <p className="mt-2 text-sm text-[var(--text-secondary)]">
              Required columns: <span className="font-mono text-xs">sourceSystem, externalLoanId, borrowerName, principalAmount, startDate, status</span>
            </p>
            <p className="mt-1 text-sm text-[var(--text-secondary)]">
              Optional column: <span className="font-mono text-xs">sourceUpdatedAt</span>
            </p>
            <label className="mt-4 block rounded-md border border-dashed border-[var(--border-default)] bg-[var(--bg-surface-2)] p-4">
              <span className="block text-sm font-semibold">Choose loan import CSV</span>
              <span className="mt-1 block text-xs text-[var(--text-secondary)]">
                CSV only, max 5MB. Preview runs immediately after file selection.
              </span>
              <input className="mt-3 block w-full text-sm" type="file" accept=".csv" onChange={onFileChange} disabled={busy} />
            </label>
            {selectedFileName ? <p className="mt-3 text-xs text-[var(--text-secondary)]">Latest file: {selectedFileName}</p> : null}
          </Surface>

          <Surface className="p-5">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="panel-title">Preview / Results</h2>
                <p className="mt-1 text-sm text-[var(--text-secondary)]">
                  Review row actions before executing the import.
                </p>
              </div>
              <Button onClick={() => void onRunImport()} disabled={!preview || busy || preview.batch.status !== "PREVIEW_READY"}>
                Run Import
              </Button>
            </div>

            {preview ? (
              <div className="mt-4 space-y-4">
                <div className="grid gap-2 md:grid-cols-5">
                  <Metric label="Rows" value={preview.batch.totalRows} />
                  <Metric label="Valid" value={preview.batch.validRows} />
                  <Metric label="Invalid" value={preview.batch.invalidRows} />
                  <Metric label="Creates" value={preview.batch.createdCount} />
                  <Metric label="Updates" value={preview.batch.updatedCount} />
                </div>
                <div className="overflow-x-auto">
                  <table className="table-base">
                    <thead>
                      <tr>
                        <th>Row</th>
                        <th>Source</th>
                        <th>External ID</th>
                        <th>Borrower</th>
                        <th>Action</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.rows.map((row) => (
                        <tr key={row.id}>
                          <td>{row.rowNumber}</td>
                          <td>{row.sourceSystem ?? "-"}</td>
                          <td className="font-mono text-xs">{row.externalLoanId ?? "-"}</td>
                          <td>{row.borrowerName ?? "-"}</td>
                          <td>{row.action}</td>
                          <td>{row.validationMessage ?? "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <p className="mt-4 text-sm text-[var(--text-secondary)]">Upload a CSV to generate a preview batch.</p>
            )}
          </Surface>
        </div>

        <div className="space-y-4">
          <Surface className="p-5">
            <h2 className="panel-title">Import History</h2>
            <div className="mt-3 space-y-2">
              {history.length ? (
                history.map((batch) => (
                  <button
                    key={batch.id}
                    type="button"
                    onClick={() => setSelectedBatchId(batch.id)}
                    className={`w-full rounded-md border p-3 text-left transition-colors ${
                      selectedBatchId === batch.id
                        ? "border-[var(--accent-primary)] bg-[var(--accent-soft)]"
                        : "border-[var(--border-default)] bg-[var(--bg-surface-2)]"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-semibold">Batch #{batch.id}</p>
                      <span className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">{batch.status}</span>
                    </div>
                    <p className="mt-1 text-xs text-[var(--text-secondary)]">{batch.fileName}</p>
                    <p className="mt-1 text-xs text-[var(--text-secondary)]">
                      {batch.sourceSystem ?? "Unknown source"} | {batch.createdCount} create | {batch.updatedCount} update | {batch.failedCount} failed
                    </p>
                  </button>
                ))
              ) : (
                <p className="text-sm text-[var(--text-secondary)]">No import batches yet.</p>
              )}
            </div>
          </Surface>

          <Surface className="p-5">
            <h2 className="panel-title">Selected Batch</h2>
            {activeBatch ? (
              <>
                <div className="mt-3 grid gap-2 md:grid-cols-2">
                  <Metric label="Batch" value={`#${activeBatch.id}`} />
                  <Metric label="Status" value={activeBatch.status} />
                  <Metric label="Rows" value={activeBatch.totalRows} />
                  <Metric label="Failed" value={activeBatch.failedCount} />
                </div>
                <div className="mt-4 max-h-[360px] overflow-auto rounded-md border border-[var(--border-default)]">
                  <table className="table-base">
                    <thead>
                      <tr>
                        <th>Row</th>
                        <th>Action</th>
                        <th>Loan</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeRows.map((row) => (
                        <tr key={row.id}>
                          <td>{row.rowNumber}</td>
                          <td>{row.action}</td>
                          <td>{row.loanId ? `#${row.loanId}` : "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : (
              <p className="mt-3 text-sm text-[var(--text-secondary)]">Select a batch to inspect its row outcomes.</p>
            )}
          </Surface>
        </div>
      </div>
      {error ? <p className="text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-md border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
      <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">{label}</p>
      <p className="mt-1 text-sm font-semibold">{value}</p>
    </div>
  );
}
