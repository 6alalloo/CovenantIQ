import type {
  ChangeRequest,
  CollateralAsset,
  CovenantException,
  ReleaseBatch,
} from "../types/api";
import { request } from "./core/http";

export function getCollaterals(loanId: number) {
  return request<CollateralAsset[]>(`/loans/${loanId}/collaterals`);
}

export function createCollateral(
  loanId: number,
  payload: {
    assetType: string;
    nominalValue: number;
    haircutPct: number;
    lienRank: number;
    currency: string;
    effectiveDate: string;
    description?: string;
  }
) {
  return request<CollateralAsset>(`/loans/${loanId}/collaterals`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCovenantExceptions(loanId: number) {
  return request<CovenantException[]>(`/loans/${loanId}/exceptions`);
}

export function createCovenantException(
  loanId: number,
  payload: {
    covenantId: number;
    exceptionType: "WAIVER" | "OVERRIDE";
    reason: string;
    effectiveFrom: string;
    effectiveTo: string;
  }
) {
  return request<CovenantException>(`/loans/${loanId}/exceptions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function approveException(exceptionId: number) {
  return request<CovenantException>(`/exceptions/${exceptionId}/approve`, { method: "PATCH" });
}

export function expireException(exceptionId: number) {
  return request<CovenantException>(`/exceptions/${exceptionId}/expire`, { method: "PATCH" });
}

export function getChangeRequests() {
  return request<ChangeRequest[]>("/change-requests");
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
  return request<ChangeRequest>("/change-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function approveChangeRequest(id: number) {
  return request<ChangeRequest>(`/change-requests/${id}/approve`, { method: "PATCH" });
}

export function getReleases() {
  return request<ReleaseBatch[]>("/releases");
}

export function createRelease(payload: { changeRequestId: number; releaseTag: string }) {
  return request<ReleaseBatch>("/releases", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function rollbackRelease(releaseId: number, payload: { targetReleaseId: number; justification: string }) {
  return request<ReleaseBatch>(`/releases/${releaseId}/rollback`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
