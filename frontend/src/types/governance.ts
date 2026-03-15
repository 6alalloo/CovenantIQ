export type CovenantExceptionType = "WAIVER" | "OVERRIDE";
export type CovenantExceptionStatus = "REQUESTED" | "APPROVED" | "REJECTED" | "EXPIRED";

export type CollateralAsset = {
  id: number;
  loanId: number;
  assetType: string;
  description: string | null;
  nominalValue: string;
  haircutPct: string;
  netEligibleValue: string;
  lienRank: number;
  currency: string;
  effectiveDate: string;
  createdAt: string;
};

export type CovenantException = {
  id: number;
  loanId: number;
  covenantId: number;
  exceptionType: CovenantExceptionType;
  reason: string;
  effectiveFrom: string;
  effectiveTo: string;
  status: CovenantExceptionStatus;
  requestedBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  controlsJson: string;
  createdAt: string;
};

export type ChangeRequestType = "RULESET" | "WORKFLOW" | "INTEGRATION_CONFIG";
export type ChangeRequestStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "RELEASED" | "ROLLED_BACK";

export type ChangeRequestItem = {
  id: number;
  artifactType: string;
  artifactId: number;
  fromVersion: string | null;
  toVersion: string | null;
  diffJson: string;
};

export type ChangeRequest = {
  id: number;
  type: ChangeRequestType;
  status: ChangeRequestStatus;
  requestedBy: string;
  requestedAt: string;
  approvedBy: string | null;
  approvedAt: string | null;
  justification: string;
  items: ChangeRequestItem[];
};

export type ReleaseAudit = {
  id: number;
  action: string;
  actor: string;
  detailsJson: string;
  timestampUtc: string;
};

export type ReleaseBatch = {
  id: number;
  changeRequestId: number;
  releaseTag: string;
  releasedBy: string;
  releasedAt: string;
  rollbackOfReleaseId: number | null;
  audits: ReleaseAudit[];
};
