import { FormEvent, useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { bulkImportStatements, getCovenantResults, submitStatement } from "../api/client";
import type { CovenantResult, PeriodType } from "../types/api";
import { Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { formatDateTime, formatEnumLabel } from "../lib/format";

export function LoanStatementsPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [form, setForm] = useState({
    periodType: "QUARTERLY" as PeriodType,
    fiscalYear: String(new Date().getFullYear()),
    fiscalQuarter: "1",
    currentAssets: "",
    currentLiabilities: "",
    totalDebt: "",
    totalEquity: "",
    ebit: "",
    interestExpense: "",
  });

  const load = async () => {
    try {
      const page = await getCovenantResults(numericLoanId, 0, 100);
      setResults(page.content);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [numericLoanId]);

  const statementHistory = useMemo(() => {
    const map = new Map<number, CovenantResult>();
    results.forEach((item) => {
      if (!map.has(item.financialStatementId)) {
        map.set(item.financialStatementId, item);
      }
    });
    return [...map.values()];
  }, [results]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    try {
      await submitStatement(numericLoanId, {
        periodType: form.periodType,
        fiscalYear: Number(form.fiscalYear),
        fiscalQuarter: form.periodType === "QUARTERLY" ? Number(form.fiscalQuarter) : null,
        currentAssets: Number(form.currentAssets),
        currentLiabilities: Number(form.currentLiabilities),
        totalDebt: Number(form.totalDebt),
        totalEquity: Number(form.totalEquity),
        ebit: Number(form.ebit),
        interestExpense: Number(form.interestExpense),
      });
      setMessage("Statement submitted successfully.");
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const handleBulkImport = async () => {
    if (!file) return;
    setError(null);
    setMessage(null);
    try {
      const summary = await bulkImportStatements(numericLoanId, file);
      setMessage(`Bulk import complete: ${summary.successCount} succeeded, ${summary.failureCount} failed.`);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="grid gap-3 xl:grid-cols-[1.2fr_1fr]">
      <Surface className="p-5">
        <h2 className="panel-title">Statement History</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">
          History is inferred from evaluation results until statement-list endpoint is added.
        </p>
        <table className="table-base mt-3">
          <thead>
            <tr>
              <th>Statement ID</th>
              <th>Last Covenant</th>
              <th>Evaluated At</th>
            </tr>
          </thead>
          <tbody>
            {statementHistory.map((item) => (
              <tr key={item.financialStatementId}>
                <td className="font-numeric">{item.financialStatementId}</td>
                <td>{formatEnumLabel(item.covenantType)}</td>
                <td>{formatDateTime(item.evaluationTimestampUtc)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Surface>

      <Surface className="p-5">
        <h2 className="panel-title">Submit Statement</h2>
        <form className="mt-3 space-y-2" onSubmit={handleSubmit}>
          <Select
            value={form.periodType}
            onChange={(event) => setForm({ ...form, periodType: event.target.value as PeriodType })}
          >
            <option value="QUARTERLY">{formatEnumLabel("QUARTERLY")}</option>
            <option value="ANNUAL">{formatEnumLabel("ANNUAL")}</option>
          </Select>
          <Input
            type="number"
            placeholder="Fiscal year"
            value={form.fiscalYear}
            onChange={(event) => setForm({ ...form, fiscalYear: event.target.value })}
          />
          {form.periodType === "QUARTERLY" ? (
            <Select
              value={form.fiscalQuarter}
              onChange={(event) => setForm({ ...form, fiscalQuarter: event.target.value })}
            >
              <option value="1">Q1</option>
              <option value="2">Q2</option>
              <option value="3">Q3</option>
              <option value="4">Q4</option>
            </Select>
          ) : null}
          <Input
            placeholder="Current assets"
            value={form.currentAssets}
            onChange={(event) => setForm({ ...form, currentAssets: event.target.value })}
            type="number"
          />
          <Input
            placeholder="Current liabilities"
            value={form.currentLiabilities}
            onChange={(event) => setForm({ ...form, currentLiabilities: event.target.value })}
            type="number"
          />
          <Input
            placeholder="Total debt"
            value={form.totalDebt}
            onChange={(event) => setForm({ ...form, totalDebt: event.target.value })}
            type="number"
          />
          <Input
            placeholder="Total equity"
            value={form.totalEquity}
            onChange={(event) => setForm({ ...form, totalEquity: event.target.value })}
            type="number"
          />
          <Input
            placeholder="EBIT"
            value={form.ebit}
            onChange={(event) => setForm({ ...form, ebit: event.target.value })}
            type="number"
          />
          <Input
            placeholder="Interest expense"
            value={form.interestExpense}
            onChange={(event) => setForm({ ...form, interestExpense: event.target.value })}
            type="number"
          />
          <Button className="w-full" type="submit">
            Submit
          </Button>
        </form>

        <h3 className="mt-5 text-sm font-semibold uppercase tracking-[0.08em] text-[var(--text-secondary)]">Bulk Import (CSV/XLSX)</h3>
        <Input className="mt-2" type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        <Button variant="outline" className="mt-2 w-full" type="button" onClick={() => void handleBulkImport()}>
          Run Bulk Import
        </Button>
      </Surface>

      {error ? <p className="text-sm text-[var(--risk-high)]">{error}</p> : null}
      {message ? <p className="text-sm text-[var(--risk-low)]">{message}</p> : null}
    </div>
  );
}
