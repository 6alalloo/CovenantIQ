import type {
  ChangeRequest,
  CollateralAsset,
  CovenantException,
  ReleaseBatch,
  Ruleset,
  RulesetValidationResult,
  RulesetVersion,
} from "../types/api";
import { request } from "./core/http";

export function createRuleset(payload: { key: string; name: string; domain: "COVENANT_EVAL"; ownerRole: string }) {
  return request<Ruleset>("/rulesets", { method: "POST", body: JSON.stringify(payload) });
}

export function getRulesets() {
  return request<Ruleset[]>("/rulesets");
}

export function createRulesetVersion(
  rulesetId: number,
  payload: { definitionJson: string; schemaVersion: number; changeSummary?: string }
) {
  return request<RulesetVersion>(`/rulesets/${rulesetId}/versions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function validateRulesetVersion(
  rulesetId: number,
  version: number,
  payload: { input?: Record<string, unknown>; expectedOutput?: Record<string, unknown> } = {}
) {
  return request<RulesetValidationResult>(`/rulesets/${rulesetId}/versions/${version}/validate`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function publishRulesetVersion(rulesetId: number, version: number, reason: string) {
  return request<RulesetVersion>(`/rulesets/${rulesetId}/versions/${version}/publish`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getRulesetVersions(rulesetId: number) {
  return request<RulesetVersion[]>(`/rulesets/${rulesetId}/versions`);
}

export function createCollateral(
  loanId: number,
  payload: {
    assetType: string;
    description?: string;
    nominalValue: number;
    haircutPct: number;
    lienRank: number;
    currency: string;
    effectiveDate: string;
  }
) {
  return request<CollateralAsset>(`/loans/${loanId}/collaterals`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCollaterals(loanId: number) {
  return request<CollateralAsset[]>(`/loans/${loanId}/collaterals`);
}

export function createCovenantException(
  loanId: number,
  payload: {
    covenantId: number;
    exceptionType: "WAIVER" | "OVERRIDE";
    reason: string;
    effectiveFrom: string;
    effectiveTo: string;
    controlsJson?: string;
  }
) {
  return request<CovenantException>(`/loans/${loanId}/exceptions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCovenantExceptions(loanId: number) {
  return request<CovenantException[]>(`/loans/${loanId}/exceptions`);
}

export function approveException(id: number) {
  return request<CovenantException>(`/exceptions/${id}/approve`, { method: "PATCH" });
}

export function expireException(id: number) {
  return request<CovenantException>(`/exceptions/${id}/expire`, { method: "PATCH" });
}

export function createChangeRequest(payload: {
  type: "RULESET" | "WORKFLOW" | "INTEGRATION_CONFIG";
  justification: string;
  items: Array<{
    artifactType: string;
    artifactId: number;
    fromVersion?: string;
    toVersion?: string;
    diffJson: string;
  }>;
}) {
  return request<ChangeRequest>("/change-requests", { method: "POST", body: JSON.stringify(payload) });
}

export function approveChangeRequest(id: number) {
  return request<ChangeRequest>(`/change-requests/${id}/approve`, { method: "PATCH" });
}

export function getChangeRequests() {
  return request<ChangeRequest[]>("/change-requests");
}

export function createRelease(payload: { changeRequestId: number; releaseTag: string }) {
  return request<ReleaseBatch>("/releases", { method: "POST", body: JSON.stringify(payload) });
}

export function rollbackRelease(id: number, payload: { targetReleaseId: number; justification: string }) {
  return request<ReleaseBatch>(`/releases/${id}/rollback`, { method: "POST", body: JSON.stringify(payload) });
}

export function getReleases() {
  return request<ReleaseBatch[]>("/releases");
}
