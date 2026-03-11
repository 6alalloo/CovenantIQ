export type WorkflowDefinition = {
  id: number;
  entityType: string;
  name: string;
  version: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  createdBy: string;
  createdAt: string;
  states: Array<{ stateCode: string; initial: boolean; terminal: boolean }>;
  transitions: Array<{ fromState: string; toState: string; allowedRoles: string[]; requiredFields: string[] }>;
};

export type WorkflowInstance = {
  id: number;
  entityType: string;
  entityId: number;
  workflowDefinitionId: number;
  currentState: string;
  startedAt: string;
  updatedAt: string;
  transitionLog: Array<{
    id: number;
    fromState: string;
    toState: string;
    actor: string;
    reason: string;
    metadataJson: string;
    timestampUtc: string;
  }>;
};
