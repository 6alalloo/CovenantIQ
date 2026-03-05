import { FormEvent, useEffect, useState } from "react";
import {
  createRuleset,
  createRulesetVersion,
  getRulesetVersions,
  getRulesets,
  publishRulesetVersion,
  validateRulesetVersion,
} from "../api/client";
import type { Ruleset, RulesetValidationResult, RulesetVersion } from "../types/api";
import { PageSection } from "../components/layout";

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

export function PolicyStudioPage() {
  const [rulesets, setRulesets] = useState<Ruleset[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [versions, setVersions] = useState<RulesetVersion[]>([]);
  const [validation, setValidation] = useState<RulesetValidationResult | null>(null);
  const [definitionJson, setDefinitionJson] = useState(SAMPLE_RULE_JSON);
  const [error, setError] = useState<string | null>(null);

  const loadRulesets = async () => {
    try {
      const all = await getRulesets();
      setRulesets(all);
      if (!selected && all.length > 0) setSelected(all[0].id);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void loadRulesets();
  }, []);

  useEffect(() => {
    if (!selected) return;
    void getRulesetVersions(selected).then(setVersions).catch((e) => setError((e as Error).message));
  }, [selected]);

  const onCreateRuleset = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createRuleset({ key: `CUSTOM_${Date.now()}`, name: "Custom Covenant Rules", domain: "COVENANT_EVAL", ownerRole: "RISK_LEAD" });
      await loadRulesets();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <PageSection title="Policy Studio" subtitle="Ruleset versions, validation harness, and publish control.">
      {error && <p className="mb-3 text-sm text-[var(--accent-danger)]">{error}</p>}
      <form onSubmit={onCreateRuleset} className="mb-4">
        <button className="btn-primary" type="submit">Create Ruleset</button>
      </form>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card space-y-2">
          <h3 className="text-sm font-semibold">Rulesets</h3>
          {rulesets.map((ruleset) => (
            <button key={ruleset.id} className="btn-secondary w-full" onClick={() => setSelected(ruleset.id)} type="button">
              {ruleset.key}
            </button>
          ))}
        </div>
        <div className="card space-y-2">
          <h3 className="text-sm font-semibold">Draft Editor</h3>
          <textarea className="input min-h-44" value={definitionJson} onChange={(e) => setDefinitionJson(e.target.value)} />
          {selected && (
            <div className="flex gap-2">
              <button
                className="btn-primary"
                onClick={async () => {
                  await createRulesetVersion(selected, { definitionJson, schemaVersion: 1, changeSummary: "Updated JSON DSL" });
                  setVersions(await getRulesetVersions(selected));
                }}
                type="button"
              >
                Save Version
              </button>
            </div>
          )}
        </div>
      </div>
      <div className="mt-4 grid gap-3">
        {versions.map((version) => (
          <div key={version.id} className="card">
            <p className="text-sm font-semibold">v{version.version} | {version.status}</p>
            <div className="mt-2 flex gap-2">
              <button
                className="btn-secondary"
                onClick={async () => {
                  setValidation(
                    await validateRulesetVersion(version.rulesetId, version.version, {
                      input: { comparisonPass: false, activeException: true },
                      expectedOutput: { breach: false },
                    })
                  );
                }}
                type="button"
              >
                Validate
              </button>
              {version.status !== "PUBLISHED" && (
                <button className="btn-secondary" onClick={() => publishRulesetVersion(version.rulesetId, version.version, "Approved for release")} type="button">
                  Publish
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
      {validation && (
        <div className="card mt-4">
          <p className="text-sm font-semibold">Validation: {validation.message}</p>
          <pre className="mt-2 overflow-auto text-xs">{JSON.stringify(validation.actualOutput, null, 2)}</pre>
        </div>
      )}
    </PageSection>
  );
}
