import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { addCovenant, createChangeRequest, getCovenants, getLoan, getRiskDetails, updateCovenant } from "../api/client";
import type { ComparisonType, Covenant, CovenantType, Loan, RiskDetails, SeverityLevel } from "../types/api";
import { Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { ConfirmDialog } from "../components/ui/confirm-dialog";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { formatEnumLabel, formatNumber } from "../lib/format";

type CovenantTemplate = {
  type: CovenantType;
  category: "Liquidity" | "Leverage" | "Coverage" | "Capital";
  description: string;
  defaultThreshold: string;
  comparisonType: ComparisonType;
  severityLevel: SeverityLevel;
  guidance: string;
};

const COVENANT_TEMPLATES: CovenantTemplate[] = [
  {
    type: "CURRENT_RATIO",
    category: "Liquidity",
    description: "Current assets relative to current liabilities.",
    defaultThreshold: "1.20",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "HIGH",
    guidance: "Common operating floor: 1.20x to 1.50x",
  },
  {
    type: "QUICK_RATIO",
    category: "Liquidity",
    description: "Liquid assets excluding inventory vs current liabilities.",
    defaultThreshold: "1.00",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
    guidance: "Typical minimum: 1.00x",
  },
  {
    type: "DEBT_TO_EQUITY",
    category: "Leverage",
    description: "Debt load relative to equity base.",
    defaultThreshold: "2.50",
    comparisonType: "LESS_THAN_EQUAL",
    severityLevel: "HIGH",
    guidance: "Lower values indicate healthier capitalization",
  },
  {
    type: "DEBT_TO_EBITDA",
    category: "Leverage",
    description: "Debt burden relative to earnings before non-cash charges.",
    defaultThreshold: "4.00",
    comparisonType: "LESS_THAN_EQUAL",
    severityLevel: "HIGH",
    guidance: "Common upper band: 3.50x to 4.50x",
  },
  {
    type: "DSCR",
    category: "Coverage",
    description: "Debt service capacity from operating income.",
    defaultThreshold: "1.25",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "HIGH",
    guidance: "Typical covenant floor: 1.20x to 1.30x",
  },
  {
    type: "INTEREST_COVERAGE",
    category: "Coverage",
    description: "Ability to cover interest from operating earnings.",
    defaultThreshold: "2.00",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
    guidance: "Lower than 2.00x typically warrants escalation",
  },
  {
    type: "FIXED_CHARGE_COVERAGE",
    category: "Coverage",
    description: "Coverage of fixed financing obligations.",
    defaultThreshold: "1.35",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
    guidance: "Frequent floor used in sponsor-backed deals",
  },
  {
    type: "TANGIBLE_NET_WORTH",
    category: "Capital",
    description: "Net worth excluding intangibles.",
    defaultThreshold: "1000000",
    comparisonType: "GREATER_THAN_EQUAL",
    severityLevel: "MEDIUM",
    guidance: "Often calibrated to deal size and sector",
  },
];

const TEMPLATE_BY_TYPE: Record<CovenantType, CovenantTemplate> = COVENANT_TEMPLATES.reduce(
  (acc, template) => {
    acc[template.type] = template;
    return acc;
  },
  {} as Record<CovenantType, CovenantTemplate>
);

const TEMPLATE_CATEGORIES: CovenantTemplate["category"][] = ["Liquidity", "Leverage", "Coverage", "Capital"];
type RiskDetailItem = RiskDetails["details"][number];

export function LoanOverviewPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [loan, setLoan] = useState<Loan | null>(null);
  const [covenants, setCovenants] = useState<Covenant[]>([]);
  const [riskDetails, setRiskDetails] = useState<RiskDetails | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editingCovenantId, setEditingCovenantId] = useState<number | null>(null);
  const [changeNotice, setChangeNotice] = useState<{ open: boolean; requestId: number | null }>({
    open: false,
    requestId: null,
  });
  const [form, setForm] = useState<{
    type: CovenantType;
    thresholdValue: string;
    comparisonType: ComparisonType;
    severityLevel: SeverityLevel;
  }>({
    type: "CURRENT_RATIO",
    thresholdValue: TEMPLATE_BY_TYPE.CURRENT_RATIO.defaultThreshold,
    comparisonType: TEMPLATE_BY_TYPE.CURRENT_RATIO.comparisonType,
    severityLevel: TEMPLATE_BY_TYPE.CURRENT_RATIO.severityLevel,
  });

  const load = async () => {
    try {
      setError(null);
      const [loanData, covenantsData] = await Promise.all([
        getLoan(numericLoanId),
        getCovenants(numericLoanId),
      ]);
      setLoan(loanData);
      setCovenants(covenantsData);
      try {
        const riskData = await getRiskDetails(numericLoanId);
        setRiskDetails(riskData);
      } catch {
        // Fresh loans may not have statement-driven risk data yet.
        setRiskDetails(null);
      }
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [numericLoanId]);

  const existingTypes = useMemo(() => new Set(covenants.map((covenant) => covenant.type)), [covenants]);
  const covenantByType = useMemo(
    () =>
      covenants.reduce<Record<CovenantType, Covenant>>((acc, covenant) => {
        acc[covenant.type] = covenant;
        return acc;
      }, {} as Record<CovenantType, Covenant>),
    [covenants]
  );
  const detailByType = useMemo(
    () =>
      (riskDetails?.details ?? []).reduce<Record<CovenantType, RiskDetailItem>>(
        (acc, detail) => {
          acc[detail.covenantType] = detail;
          return acc;
        },
        {} as Record<CovenantType, RiskDetailItem>
      ),
    [riskDetails]
  );

  const selectedTemplate = TEMPLATE_BY_TYPE[form.type];
  const selectedDetail = detailByType[form.type] ?? null;
  const selectedCovenant = covenantByType[form.type] ?? null;
  const isAlreadyAdded = existingTypes.has(form.type);
  const isEditing = editingCovenantId !== null;
  const isAlreadyAddedButNotEditing = isAlreadyAdded && !isEditing;
  const isUsingTemplateDefaults =
    form.thresholdValue === selectedTemplate.defaultThreshold &&
    form.comparisonType === selectedTemplate.comparisonType &&
    form.severityLevel === selectedTemplate.severityLevel;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (isAlreadyAddedButNotEditing) {
      setError(`Loan ${numericLoanId} already has covenant type ${form.type}`);
      return;
    }
    try {
      setError(null);
      if (isEditing && editingCovenantId) {
        const prior = selectedCovenant;
        await updateCovenant(numericLoanId, editingCovenantId, {
          thresholdValue: Number(form.thresholdValue),
          comparisonType: form.comparisonType,
          severityLevel: form.severityLevel,
        });
        if (prior) {
          const request = await createChangeRequest({
            type: "RULESET",
            justification: `Covenant rule updated for ${loan?.borrowerName ?? `Loan #${numericLoanId}`}.`,
            items: [
              {
                artifactType: "COVENANT_RULE",
                artifactId: prior.id,
                fromVersion: "current",
                toVersion: "pending",
                diffJson: JSON.stringify(
                  {
                    loanId: numericLoanId,
                    covenantType: form.type,
                    from: {
                      thresholdValue: prior.thresholdValue,
                      comparisonType: prior.comparisonType,
                      severityLevel: prior.severityLevel,
                    },
                    to: {
                      thresholdValue: form.thresholdValue,
                      comparisonType: form.comparisonType,
                      severityLevel: form.severityLevel,
                    },
                  },
                  null,
                  2
                ),
              },
            ],
          });
          setChangeNotice({ open: true, requestId: request.id });
        }
      } else {
        await addCovenant(numericLoanId, {
          type: form.type,
          thresholdValue: Number(form.thresholdValue),
          comparisonType: form.comparisonType,
          severityLevel: form.severityLevel,
        });
      }
      await load();
      if (!isEditing) {
        const nextType =
          COVENANT_TEMPLATES.find((template) => !existingTypes.has(template.type) && template.type !== form.type)
            ?.type ?? form.type;
        const nextTemplate = TEMPLATE_BY_TYPE[nextType];
        setForm({
          type: nextType,
          thresholdValue: nextTemplate.defaultThreshold,
          comparisonType: nextTemplate.comparisonType,
          severityLevel: nextTemplate.severityLevel,
        });
        setEditingCovenantId(null);
      }
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const applyTemplate = (type: CovenantType) => {
    const existing = covenantByType[type];
    if (existing) {
      setForm({
        type,
        thresholdValue: existing.thresholdValue,
        comparisonType: existing.comparisonType,
        severityLevel: existing.severityLevel,
      });
      setEditingCovenantId(existing.id);
      setError(null);
      return;
    }
    const template = TEMPLATE_BY_TYPE[type];
    setForm({
      type,
      thresholdValue: template.defaultThreshold,
      comparisonType: template.comparisonType,
      severityLevel: template.severityLevel,
    });
    setEditingCovenantId(null);
    setError(null);
  };

  const previewStatus = useMemo(() => {
    if (!selectedDetail) return null;
    const actual = Number(selectedDetail.actualValue);
    const threshold = Number(form.thresholdValue);
    if (Number.isNaN(actual) || Number.isNaN(threshold)) return null;
    const passing =
      form.comparisonType === "GREATER_THAN_EQUAL" ? actual >= threshold : actual <= threshold;
    return passing ? "PASS" : "BREACH";
  }, [form.comparisonType, form.thresholdValue, selectedDetail]);

  const navigate = useNavigate();

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
            {loan.syncManaged ? <Detail label="Source" value={`${loan.sourceSystem ?? "External"} | ${loan.externalLoanId ?? "No ID"}`} mono /> : null}
            {loan.syncManaged && loan.lastSyncedAt ? <Detail label="Last Sync" value={new Date(loan.lastSyncedAt).toLocaleString()} /> : null}
          </div>
        ) : null}

        <h3 className="mt-5 text-sm font-semibold uppercase tracking-[0.08em] text-[var(--text-secondary)]">Covenant List</h3>
        <table className="table-base mt-2">
          <thead>
            <tr>
              <th>Type</th>
              <th>Actual</th>
              <th>Threshold</th>
              <th>Rule</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {covenants.map((covenant) => {
              const detail = detailByType[covenant.type];
              return (
              <tr key={covenant.id}>
                <td>{formatEnumLabel(covenant.type)}</td>
                <td className="font-numeric">{formatNumber(detail?.actualValue)}</td>
                <td className="font-numeric">{formatNumber(covenant.thresholdValue)}</td>
                <td>{covenant.comparisonType === "GREATER_THAN_EQUAL" ? "Must stay above" : "Must stay below"}</td>
                <td><Badge>{detail ? formatEnumLabel(detail.resultStatus) : "No Evaluation"}</Badge></td>
                <td className="text-right">
                  <Button variant="outline" type="button" onClick={() => applyTemplate(covenant.type)}>
                    Edit
                  </Button>
                </td>
              </tr>
              );
            })}
          </tbody>
        </table>
      </Surface>

      <Surface className="p-5">
        <h2 className="panel-title">Add Covenant Rule</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">
          Pick a template first, then adjust rule settings if needed.
        </p>

        <div className="mt-3 space-y-3">
          {TEMPLATE_CATEGORIES.map((category) => {
            const items = COVENANT_TEMPLATES.filter((template) => template.category === category);
            return (
              <div key={category}>
                <p className="mb-2 text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">{category}</p>
                <div className="grid gap-2 sm:grid-cols-2">
                  {items.map((template) => {
                    const isSelected = form.type === template.type;
                    const exists = existingTypes.has(template.type);
                    return (
                      <button
                        key={template.type}
                        type="button"
                        onClick={() => applyTemplate(template.type)}
                        className={`rounded-md border p-2 text-left transition-colors ${
                          isSelected
                            ? "border-[var(--accent-primary)] bg-[var(--accent-soft)]"
                            : "border-[var(--border-default)] bg-[var(--bg-surface-2)]"
                        } hover:border-[var(--border-strong)]`}
                      >
                        <p className="text-xs font-semibold">{formatEnumLabel(template.type)}</p>
                        <p className="mt-1 text-[11px] text-[var(--text-secondary)]">
                          {formatNumber(template.defaultThreshold)}
                        </p>
                        {exists ? (
                          <p className="mt-1 text-[10px] uppercase tracking-[0.08em] text-[var(--risk-medium)]">Existing Rule</p>
                        ) : null}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        <form className="mt-4 space-y-3 rounded-md border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3" onSubmit={onSubmit}>
          <div className="flex items-center justify-between gap-2">
            <p className="text-sm font-semibold">{formatEnumLabel(form.type)}</p>
            <Badge>{isUsingTemplateDefaults ? "Using Template Default" : "Modified"}</Badge>
          </div>
          {isEditing ? (
            <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--risk-medium)]">
              Editing existing covenant rule{selectedCovenant ? ` (#${selectedCovenant.id})` : ""}
            </p>
          ) : null}
          <p className="text-xs text-[var(--text-secondary)]">{selectedTemplate.description}</p>
          <p className="text-[11px] text-[var(--text-secondary)]">{selectedTemplate.guidance}</p>

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
            <option value="GREATER_THAN_EQUAL">Must stay above ({">="})</option>
            <option value="LESS_THAN_EQUAL">Must stay below ({"<="})</option>
          </Select>

          <div className="grid grid-cols-3 gap-2">
            {(["LOW", "MEDIUM", "HIGH"] as SeverityLevel[]).map((level) => (
              <button
                key={level}
                type="button"
                onClick={() => setForm({ ...form, severityLevel: level })}
                className={`rounded-md border px-3 py-2 text-xs font-semibold uppercase tracking-[0.08em] ${
                  form.severityLevel === level
                    ? "border-[var(--accent-primary)] bg-[var(--accent-soft)] text-[var(--text-primary)]"
                    : "border-[var(--border-default)] text-[var(--text-secondary)]"
                }`}
              >
                {formatEnumLabel(level)}
              </button>
            ))}
          </div>

          {selectedDetail ? (
            <div className="rounded-md border border-[var(--border-default)] bg-[var(--bg-surface-1)] p-2 text-xs">
              <p>
                Latest actual: <span className="font-numeric">{formatNumber(selectedDetail.actualValue)}</span>
              </p>
              {previewStatus ? (
                <p className="mt-1">
                  Preview with current threshold:{" "}
                  <span className={previewStatus === "PASS" ? "text-[var(--risk-low)]" : "text-[var(--risk-high)]"}>
                    {previewStatus}
                  </span>
                </p>
              ) : null}
            </div>
          ) : null}

          <Button className="w-full" type="submit" disabled={isAlreadyAddedButNotEditing}>
            {isEditing ? "Save Changes" : isAlreadyAddedButNotEditing ? "Select Existing Covenant to Edit" : "Save Covenant"}
          </Button>
        </form>
      </Surface>

      {error ? <p className="text-sm text-[var(--risk-high)]">{error}</p> : null}
      <ConfirmDialog
        open={changeNotice.open}
        title="Change Request Submitted"
        description={`Change request${changeNotice.requestId ? ` #${changeNotice.requestId}` : ""} is now in Change Control.`}
        confirmLabel="Open Change Control"
        cancelLabel="Close"
        onConfirm={() => {
          setChangeNotice({ open: false, requestId: null });
          navigate("/app/change-control");
        }}
        onCancel={() => setChangeNotice({ open: false, requestId: null })}
      />
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


