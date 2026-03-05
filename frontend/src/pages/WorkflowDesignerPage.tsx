import { useEffect, useState } from "react";
import { createWorkflowDefinition, getWorkflowDefinitions, publishWorkflowDefinition } from "../api/client";
import { PageSection } from "../components/layout";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";
import type { WorkflowDefinition } from "../types/api";

const DEFAULT_ALERT_WORKFLOW = {
  entityType: "ALERT",
  name: "Alert Workflow Draft",
  states: [
    { stateCode: "OPEN", initial: true, terminal: false },
    { stateCode: "ACKNOWLEDGED", initial: false, terminal: false },
    { stateCode: "UNDER_REVIEW", initial: false, terminal: false },
    { stateCode: "RESOLVED", initial: false, terminal: true },
  ],
  transitions: [
    { fromState: "OPEN", toState: "ACKNOWLEDGED", allowedRoles: ["ANALYST", "ADMIN"], requiredFields: [] },
    { fromState: "ACKNOWLEDGED", toState: "UNDER_REVIEW", allowedRoles: ["RISK_LEAD", "ADMIN"], requiredFields: [] },
    { fromState: "UNDER_REVIEW", toState: "RESOLVED", allowedRoles: ["RISK_LEAD", "ADMIN"], requiredFields: ["resolutionNotes"] },
  ],
};

export function WorkflowDesignerPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setDefinitions(await getWorkflowDefinitions("ALERT"));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <PageSection title="Workflow Designer" subtitle="Draft, publish, and review alert workflow versions.">
      {error && <p className="mb-3 text-sm text-[var(--accent-danger)]">{error}</p>}
      {sampleUxEnabled ? (
        <div className="mb-4 flex gap-2">
          <button
            className="btn-primary"
            onClick={async () => {
              await createWorkflowDefinition(DEFAULT_ALERT_WORKFLOW);
              await load();
            }}
            type="button"
          >
            Create Sample Alert Workflow Draft
          </button>
        </div>
      ) : (
        <div className="card mb-4 text-sm text-[var(--text-secondary)]">
          Sample workflow scaffolding is disabled in normal mode.
        </div>
      )}
      <div className="grid gap-3">
        {definitions.map((definition) => (
          <div key={definition.id} className="card">
            <div className="mb-2 flex items-center justify-between">
              <p className="text-sm font-semibold">
                {definition.name} v{definition.version}
              </p>
              <span className="badge">{definition.status}</span>
            </div>
            <p className="mb-2 text-xs text-[var(--text-secondary)]">{definition.transitions.length} transitions</p>
            {definition.status !== "PUBLISHED" && (
              <button className="btn-secondary" onClick={() => publishWorkflowDefinition(definition.id, sampleUxEnabled ? "Promote tested draft" : "Publish workflow definition")} type="button">
                Publish
              </button>
            )}
          </div>
        ))}
      </div>
    </PageSection>
  );
}
