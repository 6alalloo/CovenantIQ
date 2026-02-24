import { FormEvent, useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import {
  addCovenant,
  closeLoan,
  createLoan,
  getAlerts,
  getCovenantResults,
  getLoans,
  getRiskSummary,
  submitStatement,
} from "../api/client";
import type { Alert, CovenantResult, Loan, RiskSummary } from "../types/api";
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";

export function DashboardPage() {
  const { userName, logout } = useAuth();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState<number | null>(null);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [riskSummary, setRiskSummary] = useState<RiskSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const [loanForm, setLoanForm] = useState({
    borrowerName: "",
    principalAmount: "",
    startDate: "",
  });
  const [covenantForm, setCovenantForm] = useState({
    type: "CURRENT_RATIO",
    thresholdValue: "",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
  });
  const [statementForm, setStatementForm] = useState({
    periodType: "QUARTERLY",
    fiscalYear: new Date().getFullYear().toString(),
    fiscalQuarter: "1",
    currentAssets: "",
    currentLiabilities: "",
    totalDebt: "",
    totalEquity: "",
    ebit: "",
    interestExpense: "",
  });

  async function loadLoans() {
    setError(null);
    try {
      const page = await getLoans();
      setLoans(page.content);
      if (!selectedLoanId && page.content.length > 0) {
        setSelectedLoanId(page.content[0].id);
      }
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function loadMonitoring(loanId: number) {
    setError(null);
    setLoading(true);
    try {
      const [resultPage, alertPage, summary] = await Promise.all([
        getCovenantResults(loanId),
        getAlerts(loanId),
        getRiskSummary(loanId),
      ]);
      setResults(resultPage.content);
      setAlerts(alertPage.content);
      setRiskSummary(summary);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadLoans();
  }, []);

  useEffect(() => {
    if (selectedLoanId) {
      void loadMonitoring(selectedLoanId);
    }
  }, [selectedLoanId]);

  const selectedLoan = loans.find((l) => l.id === selectedLoanId) ?? null;

  const chartData = useMemo(
    () =>
      [...results]
        .reverse()
        .map((r) => ({ name: `${r.covenantType}-${r.id}`, value: Number(r.actualValue) })),
    [results]
  );

  async function onCreateLoan(event: FormEvent) {
    event.preventDefault();
    if (!loanForm.borrowerName || !loanForm.principalAmount || !loanForm.startDate) return;
    try {
      await createLoan({
        borrowerName: loanForm.borrowerName,
        principalAmount: Number(loanForm.principalAmount),
        startDate: loanForm.startDate,
      });
      setLoanForm({ borrowerName: "", principalAmount: "", startDate: "" });
      await loadLoans();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onAddCovenant(event: FormEvent) {
    event.preventDefault();
    if (!selectedLoanId || !covenantForm.thresholdValue) return;
    try {
      await addCovenant(selectedLoanId, {
        ...covenantForm,
        thresholdValue: Number(covenantForm.thresholdValue),
      });
      setCovenantForm((v) => ({ ...v, thresholdValue: "" }));
      await loadMonitoring(selectedLoanId);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onSubmitStatement(event: FormEvent) {
    event.preventDefault();
    if (!selectedLoanId) return;
    try {
      await submitStatement(selectedLoanId, {
        periodType: statementForm.periodType,
        fiscalYear: Number(statementForm.fiscalYear),
        fiscalQuarter: statementForm.periodType === "QUARTERLY" ? Number(statementForm.fiscalQuarter) : null,
        currentAssets: Number(statementForm.currentAssets),
        currentLiabilities: Number(statementForm.currentLiabilities),
        totalDebt: Number(statementForm.totalDebt),
        totalEquity: Number(statementForm.totalEquity),
        ebit: Number(statementForm.ebit),
        interestExpense: Number(statementForm.interestExpense),
      });
      await loadMonitoring(selectedLoanId);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onCloseLoan() {
    if (!selectedLoanId) return;
    try {
      await closeLoan(selectedLoanId);
      await loadLoans();
      await loadMonitoring(selectedLoanId);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="min-h-screen bg-surface text-ink">
      <header className="bg-ink text-white px-6 py-4 flex justify-between items-center">
        <div>
          <h1 className="text-xl font-semibold">CovenantIQ Analyst Console</h1>
          <p className="text-sm text-slate-200">Logged in as {userName}</p>
        </div>
        <button onClick={logout} className="rounded-lg bg-white/10 px-3 py-2 text-sm">
          Logout
        </button>
      </header>

      <main className="p-6 grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl bg-white shadow-panel p-5">
          <h2 className="font-semibold text-lg">Create Loan</h2>
          <form onSubmit={onCreateLoan} className="mt-4 space-y-3">
            <input
              className="input"
              placeholder="Borrower Name"
              value={loanForm.borrowerName}
              onChange={(e) => setLoanForm({ ...loanForm, borrowerName: e.target.value })}
            />
            <input
              className="input"
              placeholder="Principal Amount"
              type="number"
              value={loanForm.principalAmount}
              onChange={(e) => setLoanForm({ ...loanForm, principalAmount: e.target.value })}
            />
            <input
              className="input"
              type="date"
              value={loanForm.startDate}
              onChange={(e) => setLoanForm({ ...loanForm, startDate: e.target.value })}
            />
            <button className="btn-primary w-full" type="submit">
              Create
            </button>
          </form>

          <h3 className="font-semibold mt-6">Loans</h3>
          <div className="mt-2 space-y-2 max-h-64 overflow-auto">
            {loans.map((loan) => (
              <button
                key={loan.id}
                onClick={() => setSelectedLoanId(loan.id)}
                className={`w-full rounded-lg border px-3 py-2 text-left ${
                  selectedLoanId === loan.id ? "border-primary bg-teal-50" : "border-slate-200"
                }`}
              >
                <div className="font-medium">{loan.borrowerName}</div>
                <div className="text-xs text-slate-600">
                  #{loan.id} {loan.status}
                </div>
              </button>
            ))}
          </div>
        </section>

        <section className="rounded-2xl bg-white shadow-panel p-5">
          <h2 className="font-semibold text-lg">Loan Actions</h2>
          <p className="text-sm text-slate-600 mt-1">
            {selectedLoan ? `${selectedLoan.borrowerName} (${selectedLoan.status})` : "Select a loan"}
          </p>
          <button onClick={onCloseLoan} className="btn-warning mt-4 w-full" disabled={!selectedLoanId}>
            Close Loan
          </button>

          <form onSubmit={onAddCovenant} className="mt-6 space-y-3">
            <h3 className="font-semibold">Add Covenant</h3>
            <select
              className="input"
              value={covenantForm.type}
              onChange={(e) => setCovenantForm({ ...covenantForm, type: e.target.value })}
            >
              <option value="CURRENT_RATIO">CURRENT_RATIO</option>
              <option value="DEBT_TO_EQUITY">DEBT_TO_EQUITY</option>
            </select>
            <input
              className="input"
              type="number"
              placeholder="Threshold"
              value={covenantForm.thresholdValue}
              onChange={(e) => setCovenantForm({ ...covenantForm, thresholdValue: e.target.value })}
            />
            <select
              className="input"
              value={covenantForm.comparisonType}
              onChange={(e) => setCovenantForm({ ...covenantForm, comparisonType: e.target.value })}
            >
              <option value="GREATER_THAN_EQUAL">GREATER_THAN_EQUAL</option>
              <option value="LESS_THAN_EQUAL">LESS_THAN_EQUAL</option>
            </select>
            <select
              className="input"
              value={covenantForm.severityLevel}
              onChange={(e) => setCovenantForm({ ...covenantForm, severityLevel: e.target.value })}
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
            </select>
            <button className="btn-primary w-full" type="submit" disabled={!selectedLoanId}>
              Add Covenant
            </button>
          </form>

          <form onSubmit={onSubmitStatement} className="mt-6 space-y-3">
            <h3 className="font-semibold">Submit Statement</h3>
            <select
              className="input"
              value={statementForm.periodType}
              onChange={(e) => setStatementForm({ ...statementForm, periodType: e.target.value })}
            >
              <option value="QUARTERLY">QUARTERLY</option>
              <option value="ANNUAL">ANNUAL</option>
            </select>
            <input
              className="input"
              type="number"
              placeholder="Fiscal Year"
              value={statementForm.fiscalYear}
              onChange={(e) => setStatementForm({ ...statementForm, fiscalYear: e.target.value })}
            />
            {statementForm.periodType === "QUARTERLY" && (
              <select
                className="input"
                value={statementForm.fiscalQuarter}
                onChange={(e) => setStatementForm({ ...statementForm, fiscalQuarter: e.target.value })}
              >
                <option value="1">Q1</option>
                <option value="2">Q2</option>
                <option value="3">Q3</option>
                <option value="4">Q4</option>
              </select>
            )}
            <input
              className="input"
              type="number"
              placeholder="Current Assets"
              value={statementForm.currentAssets}
              onChange={(e) => setStatementForm({ ...statementForm, currentAssets: e.target.value })}
            />
            <input
              className="input"
              type="number"
              placeholder="Current Liabilities"
              value={statementForm.currentLiabilities}
              onChange={(e) => setStatementForm({ ...statementForm, currentLiabilities: e.target.value })}
            />
            <input
              className="input"
              type="number"
              placeholder="Total Debt"
              value={statementForm.totalDebt}
              onChange={(e) => setStatementForm({ ...statementForm, totalDebt: e.target.value })}
            />
            <input
              className="input"
              type="number"
              placeholder="Total Equity"
              value={statementForm.totalEquity}
              onChange={(e) => setStatementForm({ ...statementForm, totalEquity: e.target.value })}
            />
            <input
              className="input"
              type="number"
              placeholder="EBIT"
              value={statementForm.ebit}
              onChange={(e) => setStatementForm({ ...statementForm, ebit: e.target.value })}
            />
            <input
              className="input"
              type="number"
              placeholder="Interest Expense"
              value={statementForm.interestExpense}
              onChange={(e) => setStatementForm({ ...statementForm, interestExpense: e.target.value })}
            />
            <button className="btn-primary w-full" type="submit" disabled={!selectedLoanId}>
              Submit Statement
            </button>
          </form>
        </section>

        <section className="rounded-2xl bg-white shadow-panel p-5">
          <h2 className="font-semibold text-lg">Risk Overview</h2>
          {riskSummary && (
            <div className="grid grid-cols-2 gap-3 mt-3">
              <StatCard label="Total Covenants" value={riskSummary.totalCovenants} />
              <StatCard label="Breaches" value={riskSummary.breachedCount} />
              <StatCard label="Active Warnings" value={riskSummary.activeWarnings} />
              <StatCard label="Risk Level" value={riskSummary.overallRiskLevel} />
            </div>
          )}

          <div className="mt-5 h-56 rounded-lg border border-slate-200 p-2">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" hide />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#0d9488" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      </main>

      <section className="px-6 pb-6 grid gap-6 lg:grid-cols-2">
        <DataTable
          title="Covenant Results"
          headers={["ID", "Type", "Actual", "Status"]}
          rows={results.map((r) => [String(r.id), r.covenantType, r.actualValue, r.status])}
        />
        <DataTable
          title="Alerts"
          headers={["ID", "Type", "Severity", "Message"]}
          rows={alerts.map((a) => [String(a.id), a.alertType, a.severityLevel, a.message])}
        />
      </section>

      {loading && <p className="px-6 pb-6 text-sm text-slate-500">Loading...</p>}
      {error && <p className="px-6 pb-6 text-sm text-red-600">{error}</p>}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <div className="text-xs uppercase text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-semibold">{value}</div>
    </div>
  );
}

function DataTable({ title, headers, rows }: { title: string; headers: string[]; rows: string[][] }) {
  return (
    <div className="rounded-2xl bg-white shadow-panel p-5 overflow-auto">
      <h3 className="font-semibold mb-3">{title}</h3>
      <table className="w-full text-sm">
        <thead>
          <tr>
            {headers.map((h) => (
              <th key={h} className="text-left border-b pb-2 pr-3 text-slate-600">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx}>
              {row.map((cell, cidx) => (
                <td key={cidx} className="py-2 pr-3 border-b border-slate-100">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
