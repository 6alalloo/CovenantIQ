import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  createRuleset,
  createRulesetVersion,
  getRulesetVersions,
  getRulesets,
  publishRulesetVersion,
  validateRulesetVersion,
} from "../api/client";
import { PageSection } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";
import { formatDateTime, formatEnumLabel } from "../lib/format";
import type { Ruleset, RulesetValidationResult, RulesetVersion } from "../types/api";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";

const SAMPLE_RULE_JSON = `{
  "rules": [
    {
      "when": {"comparisonPass": false, "activeException": true},
      "outcome": {"breach": false, "alertType": "EARLY_WARNING", "reasonCode": "EXCEPTION_DOWNGRADED"}
    },
    {
      "when": {"comparisonPass": false},
      "outcome": {"breach": true, "alertType": "BREACH", "reasonCode": "THRESHOLD_BREACH"}
    }
  ]
}`;

type WorkspaceTab = "definition" | "tests" | "diff" | "history";

const SAMPLE_TEST_INPUT = { comparisonPass: false, activeException: true };
const SAMPLE_EXPECTED_OUTPUT = { breach: false };

function summarizeRules(definitionJson: string) {
  try {
    const parsed = JSON.parse(definitionJson) as { rules?: Array<{ when?: Record<string, unknown>; outcome?: Record<string, unknown> }> };
    const rules = Array.isArray(parsed.rules) ? parsed.rules : [];
    const alertTypes = new Set(
      rules
        .map((rule) => String(rule.outcome?.alertType ?? ""))
        .filter(Boolean)
    );
    const exceptionAware = rules.filter((rule) => Boolean(rule.when?.activeException)).length;
    const breachRules = rules.filter((rule) => rule.outcome?.breach === true).length;
    return {
      rulesCount: rules.length,
      alertTypes: alertTypes.size,
      exceptionAware,
      breachRules,
      parseError: null as string | null,
    };
  } catch (error) {
    return {
      rulesCount: 0,
      alertTypes: 0,
      exceptionAware: 0,
      breachRules: 0,
      parseError: (error as Error).message,
    };
  }
}

function safeParse(definitionJson: string) {
  try {
    return JSON.parse(definitionJson) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function describeDiff(currentJson: string, baselineJson: string | null) {
  const current = safeParse(currentJson);
  const baseline = baselineJson ? safeParse(baselineJson) : null;
  const currentRules = Array.isArray(current?.rules) ? current.rules : [];
  const baselineRules = Array.isArray(baseline?.rules) ? baseline.rules : [];

  return {
    addedRules: Math.max(0, currentRules.length - baselineRules.length),
    removedRules: Math.max(0, baselineRules.length - currentRules.length),
    changedDefinition: currentJson !== (baselineJson ?? ""),
    baselineLabel: baselineJson ? "live version" : "no live version",
  };
}

function statusTone(status: RulesetVersion["status"] | "LIVE" | "UNVALIDATED") {
  if (status === "PUBLISHED" || status === "LIVE") return "LOW";
  if (status === "VALIDATED") return "MEDIUM";
  if (status === "UNVALIDATED") return "BREACH";
  return "OPEN";
}

export function PolicyStudioPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const [rulesets, setRulesets] = useState<Ruleset[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [versions, setVersions] = useState<RulesetVersion[]>([]);
  const [validation, setValidation] = useState<RulesetValidationResult | null>(null);
  const [definitionJson, setDefinitionJson] = useState(sampleUxEnabled ? SAMPLE_RULE_JSON : "{\n  \"rules\": []\n}");
  const [changeSummary, setChangeSummary] = useState(sampleUxEnabled ? "Tune covenant breach handling for approved exceptions" : "");
  const [activeTab, setActiveTab] = useState<WorkspaceTab>("definition");
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);

  const selectedRuleset = useMemo(
    () => rulesets.find((ruleset) => ruleset.id === selected) ?? null,
    [rulesets, selected]
  );
  const publishedVersion = useMemo(
    () => versions.find((version) => version.status === "PUBLISHED") ?? null,
    [versions]
  );
  const draftVersion = useMemo(
    () => versions.find((version) => version.status === "DRAFT" || version.status === "VALIDATED") ?? null,
    [versions]
  );
  const selectedVersion = draftVersion ?? publishedVersion ?? versions[0] ?? null;
  const hasUnsavedChanges = Boolean(selectedVersion) && definitionJson !== selectedVersion.definitionJson;
  const rulesetSummary = useMemo(() => summarizeRules(definitionJson), [definitionJson]);
  const diffSummary = useMemo(
    () => describeDiff(definitionJson, publishedVersion?.definitionJson ?? null),
    [definitionJson, publishedVersion?.definitionJson]
  );

  const loadRulesets = async () => {
    try {
      setError(null);
      const all = await getRulesets();
      setRulesets(all);
      setSelected((current) => current ?? all[0]?.id ?? null);
    } catch (loadError) {
      setError((loadError as Error).message);
    }
  };

  const loadVersions = async (rulesetId: number) => {
    try {
      setError(null);
      const next = await getRulesetVersions(rulesetId);
      setVersions(next);
      const preferred = next.find((version) => version.status === "DRAFT" || version.status === "VALIDATED")
        ?? next.find((version) => version.status === "PUBLISHED")
        ?? next[0]
        ?? null;
      if (preferred) {
        setDefinitionJson(preferred.definitionJson);
        setChangeSummary(preferred.changeSummary ?? (sampleUxEnabled ? "Tune covenant breach handling for approved exceptions" : ""));
      } else {
        setDefinitionJson(sampleUxEnabled ? SAMPLE_RULE_JSON : "{\n  \"rules\": []\n}");
      }
      setValidation(null);
    } catch (loadError) {
      setError((loadError as Error).message);
    }
  };

  useEffect(() => {
    void loadRulesets();
  }, []);

  useEffect(() => {
    if (!selected) return;
    void loadVersions(selected);
  }, [selected]);

  const onCreateRuleset = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setError(null);
      await createRuleset({
        key: `CUSTOM_${Date.now()}`,
        name: "Custom Covenant Rules",
        domain: "COVENANT_EVAL",
        ownerRole: "RISK_LEAD",
      });
      await loadRulesets();
    } catch (createError) {
      setError((createError as Error).message);
    }
  };

  const handleSaveVersion = async () => {
    if (!selected) return;
    try {
      setIsSaving(true);
      setError(null);
      await createRulesetVersion(selected, {
        definitionJson,
        schemaVersion: 1,
        changeSummary,
      });
      await loadVersions(selected);
    } catch (saveError) {
      setError((saveError as Error).message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleValidate = async (version: RulesetVersion | null) => {
    if (!version) return;
    try {
      setIsValidating(true);
      setError(null);
      const result = await validateRulesetVersion(version.rulesetId, version.version, {
        input: sampleUxEnabled ? SAMPLE_TEST_INPUT : {},
        expectedOutput: sampleUxEnabled ? SAMPLE_EXPECTED_OUTPUT : {},
      });
      setValidation(result);
      setActiveTab("tests");
    } catch (validateError) {
      setError((validateError as Error).message);
    } finally {
      setIsValidating(false);
    }
  };

  const handlePublish = async (version: RulesetVersion | null) => {
    if (!version) return;
    try {
      setIsPublishing(true);
      setError(null);
      await publishRulesetVersion(version.rulesetId, version.version, sampleUxEnabled ? "Approved for release after validation review" : "Publish ruleset version");
      await loadVersions(version.rulesetId);
      setActiveTab("history");
    } catch (publishError) {
      setError((publishError as Error).message);
    } finally {
      setIsPublishing(false);
    }
  };

  return (
    <PageSection
      title="Policy Studio"
      subtitle="Author, test, compare, and prepare covenant policy changes before they enter change control."
      action={
        <form onSubmit={onCreateRuleset}>
          <Button type="submit">Create Ruleset</Button>
        </form>
      }
    >
      {error && <p className="text-sm text-[var(--risk-high)]">{error}</p>}

      <section className="policy-studio-shell">
        <aside className="governance-panel governance-sidebar">
          <div className="governance-panel__header">
            <div>
              <p className="governance-eyebrow">Ruleset Directory</p>
              <h2 className="panel-title">Covenant evaluation policies</h2>
            </div>
            <Badge>{selectedRuleset?.domain ?? "COVENANT_EVAL"}</Badge>
          </div>
          <div className="space-y-3">
            {rulesets.map((ruleset) => {
              const isActive = ruleset.id === selected;
              const relatedVersions = isActive ? versions : [];
              const live = relatedVersions.find((version) => version.status === "PUBLISHED");
              const draft = relatedVersions.find((version) => version.status === "DRAFT" || version.status === "VALIDATED");
              return (
                <button
                  key={ruleset.id}
                  className="governance-list-item text-left"
                  data-active={isActive}
                  onClick={() => setSelected(ruleset.id)}
                  type="button"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-[var(--text-primary)]">{ruleset.name}</p>
                      <p className="mt-1 text-xs text-[var(--text-secondary)]">{ruleset.key}</p>
                    </div>
                    <Badge>{draft ? draft.status : live ? "Live" : "Draft"}</Badge>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-[var(--text-secondary)]">
                    <span>Owner {formatEnumLabel(ruleset.ownerRole)}</span>
                    <span>Created {formatDateTime(ruleset.createdAt)}</span>
                    <span>{live ? `Live v${live.version}` : "No live version"}</span>
                  </div>
                </button>
              );
            })}
          </div>
        </aside>

        <div className="governance-workspace">
          <section className="governance-hero card p-5">
            <div className="governance-hero__meta">
              <div>
                <p className="governance-eyebrow">Governance / Policy Studio / Covenant Evaluation</p>
                <h2 className="text-2xl font-semibold text-[var(--text-primary)]">
                  {selectedRuleset?.name ?? "Select a ruleset"}
                </h2>
                <p className="mt-2 max-w-3xl text-sm text-[var(--text-secondary)]">
                  Keep draft work separate from live production logic. Validate expected outcomes before sending a change forward for approval.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge>{publishedVersion ? `Live v${publishedVersion.version}` : "No live version"}</Badge>
                <Badge>{hasUnsavedChanges ? "Unsaved draft" : selectedVersion ? selectedVersion.status : "Draft only"}</Badge>
              </div>
            </div>
            <div className="governance-stat-grid">
              <div className="governance-stat-card">
                <span className="governance-stat-card__label">Rules</span>
                <strong className="font-numeric text-2xl">{rulesetSummary.rulesCount}</strong>
              </div>
              <div className="governance-stat-card">
                <span className="governance-stat-card__label">Alert paths</span>
                <strong className="font-numeric text-2xl">{rulesetSummary.alertTypes}</strong>
              </div>
              <div className="governance-stat-card">
                <span className="governance-stat-card__label">Exception-aware</span>
                <strong className="font-numeric text-2xl">{rulesetSummary.exceptionAware}</strong>
              </div>
              <div className="governance-stat-card">
                <span className="governance-stat-card__label">Breach branches</span>
                <strong className="font-numeric text-2xl">{rulesetSummary.breachRules}</strong>
              </div>
            </div>
          </section>

          <section className="governance-panel p-5">
            <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as WorkspaceTab)}>
              <TabsList className="mb-5">
                <TabsTrigger value="definition">Definition</TabsTrigger>
                <TabsTrigger value="tests">Test Cases</TabsTrigger>
                <TabsTrigger value="diff">Diff</TabsTrigger>
                <TabsTrigger value="history">History</TabsTrigger>
              </TabsList>
            </Tabs>

            {activeTab === "definition" && (
              <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_280px]">
                <div className="space-y-4">
                  <div>
                    <label className="governance-label" htmlFor="changeSummary">Change Summary</label>
                    <input
                      id="changeSummary"
                      className="input"
                      value={changeSummary}
                      onChange={(event) => setChangeSummary(event.target.value)}
                      placeholder="Describe what changed and why"
                    />
                  </div>
                  <div>
                    <div className="mb-2 flex items-center justify-between gap-3">
                      <label className="governance-label" htmlFor="rulesetEditor">Definition</label>
                      <span className="text-xs text-[var(--text-secondary)]">JSON DSL draft editor</span>
                    </div>
                    <textarea
                      id="rulesetEditor"
                      className="governance-editor"
                      spellCheck={false}
                      value={definitionJson}
                      onChange={(event) => setDefinitionJson(event.target.value)}
                    />
                  </div>
                </div>
                <div className="governance-subpanel space-y-4">
                  <div>
                    <p className="governance-eyebrow">Editor Guidance</p>
                    <ul className="mt-2 space-y-2 text-sm text-[var(--text-secondary)]">
                      <li>Define one rules array with ordered conditions.</li>
                      <li>Model exception-aware branches before general breach branches.</li>
                      <li>Keep reason codes stable so downstream audit trails remain readable.</li>
                    </ul>
                  </div>
                  <div>
                    <p className="governance-eyebrow">Draft State</p>
                    <div className="mt-2 space-y-2 text-sm text-[var(--text-secondary)]">
                      <p>{hasUnsavedChanges ? "You have unsaved changes in the editor." : "Editor matches the selected stored version."}</p>
                      <p>{rulesetSummary.parseError ? `Parse error: ${rulesetSummary.parseError}` : "Definition parses successfully."}</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "tests" && (
              <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
                <div className="governance-subpanel">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="governance-eyebrow">Validation Harness</p>
                      <h3 className="panel-title">Exception downgrade scenario</h3>
                    </div>
                    <Button variant="outline" onClick={() => handleValidate(selectedVersion)} disabled={!selectedVersion || isValidating}>
                      {isValidating ? "Running..." : "Run Validation"}
                    </Button>
                  </div>
                  <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <div>
                      <p className="governance-label">Input</p>
                      <pre className="governance-code-block">{JSON.stringify(SAMPLE_TEST_INPUT, null, 2)}</pre>
                    </div>
                    <div>
                      <p className="governance-label">Expected Output</p>
                      <pre className="governance-code-block">{JSON.stringify(SAMPLE_EXPECTED_OUTPUT, null, 2)}</pre>
                    </div>
                  </div>
                </div>
                <div className="governance-subpanel">
                  <p className="governance-eyebrow">Latest Result</p>
                  {validation ? (
                    <div className="mt-3 space-y-3">
                      <Badge>{validation.pass ? "Pass" : "Breach"}</Badge>
                      <p className="text-sm text-[var(--text-secondary)]">{validation.message}</p>
                      <pre className="governance-code-block">{JSON.stringify(validation.actualOutput, null, 2)}</pre>
                    </div>
                  ) : (
                    <p className="mt-3 text-sm text-[var(--text-secondary)]">
                      Run the sample validation to confirm the draft still downgrades active exceptions before you promote it.
                    </p>
                  )}
                </div>
              </div>
            )}

            {activeTab === "diff" && (
              <div className="grid gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
                <div className="governance-subpanel space-y-4">
                  <div>
                    <p className="governance-eyebrow">Draft vs {diffSummary.baselineLabel}</p>
                    <div className="mt-3 space-y-3">
                      <div className="governance-diff-card">
                        <span className="governance-diff-card__label">Rules added</span>
                        <strong className="font-numeric text-xl">+{diffSummary.addedRules}</strong>
                      </div>
                      <div className="governance-diff-card">
                        <span className="governance-diff-card__label">Rules removed</span>
                        <strong className="font-numeric text-xl">-{diffSummary.removedRules}</strong>
                      </div>
                      <div className="governance-diff-card">
                        <span className="governance-diff-card__label">Definition changed</span>
                        <strong className="text-base">{diffSummary.changedDefinition ? "Yes" : "No"}</strong>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="governance-subpanel">
                  <p className="governance-eyebrow">Raw draft</p>
                  <pre className="governance-code-block governance-code-block--tall">{definitionJson}</pre>
                </div>
              </div>
            )}

            {activeTab === "history" && (
              <div className="space-y-4">
                {versions.length === 0 && (
                  <div className="governance-subpanel">
                    <p className="text-sm text-[var(--text-secondary)]">No stored versions yet. Save the current draft to start version history.</p>
                  </div>
                )}
                {versions.map((version) => (
                  <article key={version.id} className="governance-timeline-item">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-sm font-semibold text-[var(--text-primary)]">Version {version.version}</h3>
                          <Badge>{version.status}</Badge>
                          {version.status === "PUBLISHED" && <Badge>Live</Badge>}
                        </div>
                        <p className="mt-2 text-sm text-[var(--text-secondary)]">{version.changeSummary ?? "No change summary provided."}</p>
                      </div>
                      <div className="text-right text-xs text-[var(--text-secondary)]">
                        <p>Created {formatDateTime(version.createdAt)}</p>
                        <p>{version.approvedBy ? `Approved by ${version.approvedBy}` : `Created by ${version.createdBy}`}</p>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>

        <aside className="governance-panel governance-sidebar">
          <div className="governance-panel__header">
            <div>
              <p className="governance-eyebrow">Operational Panel</p>
              <h2 className="panel-title">Draft readiness</h2>
            </div>
            <Badge>{selectedVersion ? formatEnumLabel(selectedVersion.status) : "Draft"}</Badge>
          </div>

          <div className="governance-subpanel">
            <p className="governance-eyebrow">Validation status</p>
            <div className="mt-3 flex items-center gap-2">
              <Badge>{validation ? (validation.pass ? "Pass" : "Breach") : "Unvalidated"}</Badge>
              <span className="text-sm text-[var(--text-secondary)]">
                {validation ? validation.message : "No validation run recorded in this session."}
              </span>
            </div>
          </div>

          <div className="governance-subpanel">
            <p className="governance-eyebrow">Impact summary</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
              <div className="governance-diff-card">
                <span className="governance-diff-card__label">Behavior shift</span>
                <strong>{diffSummary.changedDefinition ? "Draft diverges from live" : "Matches live version"}</strong>
              </div>
              <div className="governance-diff-card">
                <span className="governance-diff-card__label">Approval path</span>
                <strong>{validation?.pass ? "Ready for change control" : "Validate before submit"}</strong>
              </div>
            </div>
          </div>

          <div className="governance-subpanel space-y-3">
            <p className="governance-eyebrow">Next actions</p>
            <Button onClick={handleSaveVersion} disabled={!selected || isSaving || Boolean(rulesetSummary.parseError)}>
              {isSaving ? "Saving..." : hasUnsavedChanges ? "Save Draft" : "Save Version"}
            </Button>
            <Button variant="outline" onClick={() => handleValidate(selectedVersion)} disabled={!selectedVersion || isValidating}>
              {isValidating ? "Running validation..." : "Run Validation"}
            </Button>
            <Button variant="outline" onClick={() => setActiveTab("diff")}>
              Compare to Live
            </Button>
            <Button
              variant="outline"
              onClick={() => handlePublish(selectedVersion)}
              disabled={!selectedVersion || selectedVersion.status === "PUBLISHED" || isPublishing}
            >
              {isPublishing ? "Publishing..." : selectedVersion?.status === "PUBLISHED" ? "Live Version" : "Publish Live"}
            </Button>
            <p className="text-xs text-[var(--text-secondary)]">
              Direct publish remains exposed for the current implementation, but the surface now treats validation and comparison as required preceding steps.
            </p>
          </div>
        </aside>
      </section>
    </PageSection>
  );
}


