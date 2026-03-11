export type Ruleset = {
  id: number;
  key: string;
  name: string;
  domain: "COVENANT_EVAL";
  ownerRole: string;
  createdBy: string;
  createdAt: string;
};

export type RulesetVersion = {
  id: number;
  rulesetId: number;
  version: number;
  status: "DRAFT" | "VALIDATED" | "PUBLISHED" | "ARCHIVED";
  definitionJson: string;
  schemaVersion: number;
  changeSummary: string | null;
  createdBy: string;
  approvedBy: string | null;
  publishedAt: string | null;
  createdAt: string;
};

export type RulesetValidationResult = {
  rulesetVersionId: number;
  valid: boolean;
  pass: boolean;
  actualOutput: Record<string, unknown>;
  message: string;
};

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
  exceptionType: "WAIVER" | "OVERRIDE";
  reason: string;
  effectiveFrom: string;
  effectiveTo: string;
  status: "REQUESTED" | "APPROVED" | "EXPIRED" | "REJECTED";
  requestedBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  controlsJson: string;
  createdAt: string;
};

export type ChangeRequest = {
  id: number;
  type: "RULESET" | "WORKFLOW" | "INTEGRATION_CONFIG";
  status: "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "RELEASED" | "ROLLED_BACK";
  requestedBy: string;
  requestedAt: string;
  approvedBy: string | null;
  approvedAt: string | null;
  justification: string;
  items: Array<{
    id: number;
    artifactType: string;
    artifactId: number;
    fromVersion: string | null;
    toVersion: string | null;
    diffJson: string;
  }>;
};

export type ReleaseBatch = {
  id: number;
  changeRequestId: number;
  releaseTag: string;
  releasedBy: string;
  releasedAt: string;
  rollbackOfReleaseId: number | null;
  audits: Array<{
    id: number;
    action: string;
    actor: string;
    detailsJson: string;
    timestampUtc: string;
  }>;
};
