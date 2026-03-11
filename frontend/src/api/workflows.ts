import type { WorkflowDefinition, WorkflowInstance } from "../types/api";
import { makeQuery, request } from "./core/http";

export function createWorkflowDefinition(payload: {
  entityType: string;
  name: string;
  states: Array<{ stateCode: string; initial: boolean; terminal: boolean }>;
  transitions: Array<{
    fromState: string;
    toState: string;
    allowedRoles: string[];
    requiredFields: string[];
    guardExpression?: string;
  }>;
}) {
  return request<WorkflowDefinition>("/workflows/definitions", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getWorkflowDefinitions(entityType?: string) {
  return request<WorkflowDefinition[]>(`/workflows/definitions${makeQuery({ entityType })}`);
}

export function publishWorkflowDefinition(id: number, reason: string) {
  return request<WorkflowDefinition>(`/workflows/definitions/${id}/publish`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getWorkflowInstance(entityType: string, entityId: number) {
  return request<WorkflowInstance>(`/workflows/instances/${entityType}/${entityId}`);
}

export function transitionWorkflowInstance(id: number, toState: string, reason: string, metadata?: Record<string, unknown>) {
  return request<WorkflowInstance>(`/workflows/instances/${id}/transition`, {
    method: "POST",
    body: JSON.stringify({ toState, reason, metadata: metadata ?? {} }),
  });
}
