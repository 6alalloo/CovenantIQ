import { useEffect, useState } from "react";
import { createWorkflowDefinition, getWorkflowDefinitions, publishWorkflowDefinition } from "../api/client";
import type { WorkflowDefinition } from "../types/api";
import { PageSection } from "../components/layout";

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
      <div className="mb-4 flex gap-2">
        <button
          className="btn-primary"
          onClick={async () => {
            await createWorkflowDefinition(DEFAULT_ALERT_WORKFLOW);
            await load();
          }}
          type="button"
        >
          Create Alert Workflow Draft
        </button>
      </div>
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
              <button className="btn-secondary" onClick={() => publishWorkflowDefinition(definition.id, "Promote tested draft")} type="button">
                Publish
              </button>
            )}
          </div>
        ))}
      </div>
    </PageSection>
  );
}
