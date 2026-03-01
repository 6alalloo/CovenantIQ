import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { addCovenant, getLoan, getRiskDetails } from "../api/client";
import type { ComparisonType, CovenantType, Loan, RiskDetails, SeverityLevel } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { formatEnumLabel } from "../lib/format";

const COVENANT_TYPES: CovenantType[] = [
  "CURRENT_RATIO",
  "DEBT_TO_EQUITY",
  "DSCR",
  "INTEREST_COVERAGE",
  "TANGIBLE_NET_WORTH",
  "DEBT_TO_EBITDA",
  "FIXED_CHARGE_COVERAGE",
  "QUICK_RATIO",
];

export function LoanOverviewPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [loan, setLoan] = useState<Loan | null>(null);
  const [riskDetails, setRiskDetails] = useState<RiskDetails | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<{
    type: CovenantType;
    thresholdValue: string;
    comparisonType: ComparisonType;
    severityLevel: SeverityLevel;
  }>({
    type: "CURRENT_RATIO",
    thresholdValue: "",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
  });

  const load = async () => {
    try {
      const [loanData, riskData] = await Promise.all([getLoan(numericLoanId), getRiskDetails(numericLoanId)]);
      setLoan(loanData);
      setRiskDetails(riskData);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [numericLoanId]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await addCovenant(numericLoanId, {
        type: form.type,
        thresholdValue: Number(form.thresholdValue),
        comparisonType: form.comparisonType,
        severityLevel: form.severityLevel,
      });
      setForm({ ...form, thresholdValue: "" });
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="grid gap-3 xl:grid-cols-[1.5fr_1fr]">
      <Surface className="p-5">
        <h2 className="panel-title">Borrower Snapshot</h2>
        {loan ? (
          <div className="mt-3 grid gap-2 md:grid-cols-2">
            <Detail label="Borrower" value={loan.borrowerName} />
            <Detail label="Loan ID" value={`#${loan.id}`} mono />
            <Detail label="Principal" value={`$${Number(loan.principalAmount).toLocaleString()}`} mono />
            <Detail label="Status" value={formatEnumLabel(loan.status)} />
          </div>
        ) : null}

        <h3 className="mt-5 text-sm font-semibold uppercase tracking-[0.08em] text-[var(--text-secondary)]">Covenant List</h3>
        <table className="table-base mt-2">
          <thead>
            <tr>
              <th>Type</th>
              <th>Actual</th>
              <th>Threshold</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {riskDetails?.details.map((detail) => (
              <tr key={detail.covenantId}>
                <td>{formatEnumLabel(detail.covenantType)}</td>
                <td className="font-numeric">{detail.actualValue}</td>
                <td className="font-numeric">{detail.thresholdValue}</td>
                <td><Badge>{formatEnumLabel(detail.resultStatus)}</Badge></td>
              </tr>
            )) ?? null}
          </tbody>
        </table>
      </Surface>

      <Surface className="p-5">
        <h2 className="panel-title">Add Covenant</h2>
        <form className="mt-3 space-y-3" onSubmit={onSubmit}>
          <Select
            value={form.type}
            onChange={(event) => setForm({ ...form, type: event.target.value as CovenantType })}
          >
            {COVENANT_TYPES.map((type) => (
              <option key={type} value={type}>
                {formatEnumLabel(type)}
              </option>
            ))}
          </Select>
          <Input
            className="font-numeric"
            type="number"
            step="0.01"
            placeholder="Threshold value"
            value={form.thresholdValue}
            onChange={(event) => setForm({ ...form, thresholdValue: event.target.value })}
            required
          />
          <Select
            value={form.comparisonType}
            onChange={(event) => setForm({ ...form, comparisonType: event.target.value as ComparisonType })}
          >
            <option value="GREATER_THAN_EQUAL">{formatEnumLabel("GREATER_THAN_EQUAL")}</option>
            <option value="LESS_THAN_EQUAL">{formatEnumLabel("LESS_THAN_EQUAL")}</option>
          </Select>
          <Select
            value={form.severityLevel}
            onChange={(event) => setForm({ ...form, severityLevel: event.target.value as SeverityLevel })}
          >
            <option value="LOW">{formatEnumLabel("LOW")}</option>
            <option value="MEDIUM">{formatEnumLabel("MEDIUM")}</option>
            <option value="HIGH">{formatEnumLabel("HIGH")}</option>
          </Select>
          <Button className="w-full" type="submit">
            Save Covenant
          </Button>
        </form>
      </Surface>

      {error ? <p className="text-sm text-[var(--risk-high)]">{error}</p> : null}
    </div>
  );
}

function Detail({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="rounded-md border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
      <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">{label}</p>
      <p className={`mt-1 text-sm ${mono ? "font-numeric" : ""}`}>{value}</p>
    </div>
  );
}
