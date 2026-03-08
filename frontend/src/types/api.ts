export type UserRole = "ANALYST" | "RISK_LEAD" | "ADMIN";
export type LoanStatus = "ACTIVE" | "CLOSED";
export type CovenantType =
  | "CURRENT_RATIO"
  | "DEBT_TO_EQUITY"
  | "DSCR"
  | "INTEREST_COVERAGE"
  | "TANGIBLE_NET_WORTH"
  | "DEBT_TO_EBITDA"
  | "FIXED_CHARGE_COVERAGE"
  | "QUICK_RATIO";
export type ComparisonType = "GREATER_THAN_EQUAL" | "LESS_THAN_EQUAL";
export type SeverityLevel = "LOW" | "MEDIUM" | "HIGH";
export type PeriodType = "QUARTERLY" | "ANNUAL";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type CovenantResultStatus = "PASS" | "BREACH";
export type AlertType = "BREACH" | "EARLY_WARNING";
export type AlertStatus = "OPEN" | "ACKNOWLEDGED" | "UNDER_REVIEW" | "RESOLVED";
export type ActivityEventType =
  | "LOAN_CREATED"
  | "LOAN_UPDATED"
  | "LOAN_CLOSED"
  | "LOAN_IMPORTED"
  | "LOAN_SYNC_UPDATED"
  | "COVENANT_CREATED"
  | "STATEMENT_SUBMITTED"
  | "ALERT_ACKNOWLEDGED"
  | "ALERT_RESOLVED"
  | "COMMENT_ADDED"
  | "COMMENT_DELETED"
  | "USER_CREATED"
  | "USER_UPDATED"
  | "USER_DEACTIVATED";

export type BackendMode = "NORMAL" | "DEMO" | "TEST";

export type RuntimeConfig = {
  backendMode: BackendMode;
  demoMode: boolean;
  testMode: boolean;
  sampleContentAvailable: boolean;
  strictSecretValidationEnabled: boolean;
};

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  username: string;
  roles: UserRole[];
};

export type Loan = {
  id: number;
  borrowerName: string;
  principalAmount: string;
  startDate: string;
  status: LoanStatus;
  externalLoanId: string | null;
  sourceSystem: string | null;
  lastSyncedAt: string | null;
  sourceUpdatedAt: string | null;
  syncManaged: boolean;
};

export type Covenant = {
  id: number;
  loanId: number;
  type: CovenantType;
  thresholdValue: string;
  comparisonType: ComparisonType;
  severityLevel: SeverityLevel;
};

export type FinancialStatement = {
  id: number;
  loanId: number;
  periodType: PeriodType;
  fiscalYear: number;
  fiscalQuarter: number | null;
  currentAssets: string;
  currentLiabilities: string;
  totalDebt: string;
  totalEquity: string;
  ebit: string;
  interestExpense: string;
  netOperatingIncome: string | null;
  totalDebtService: string | null;
  intangibleAssets: string | null;
  ebitda: string | null;
  fixedCharges: string | null;
  inventory: string | null;
  totalAssets: string | null;
  totalLiabilities: string | null;
  submissionTimestampUtc: string;
};

export type CovenantResult = {
  id: number;
  covenantId: number;
  covenantType: CovenantType;
  financialStatementId: number;
  actualValue: string;
  status: CovenantResultStatus;
  evaluationTimestampUtc: string;
};

export type Alert = {
  id: number;
  loanId: number;
  financialStatementId: number;
  alertType: AlertType;
  message: string;
  severityLevel: SeverityLevel;
  alertRuleCode: string;
  triggeredTimestampUtc: string;
  status: AlertStatus;
  acknowledgedBy: string | null;
  acknowledgedAt: string | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
};

export type RiskSummary = {
  totalCovenants: number;
  breachedCount: number;
  activeWarnings: number;
  overallRiskLevel: RiskLevel;
};

export type CovenantRiskDetail = {
  covenantId: number;
  covenantType: CovenantType;
  thresholdValue: string;
  actualValue: string;
  comparisonType: ComparisonType;
  resultStatus: CovenantResultStatus;
  severityLevel: SeverityLevel;
};

export type RiskDetails = {
  loanId: number;
  financialStatementId: number;
  evaluationTimestampUtc: string;
  details: CovenantRiskDetail[];
};

export type PortfolioSummary = {
  totalActiveLoans: number;
  totalBreaches: number;
  highRiskLoanCount: number;
  mediumRiskLoanCount: number;
  lowRiskLoanCount: number;
  totalOpenAlerts: number;
  totalUnderReviewAlerts: number;
};

export type ActivityLog = {
  id: number;
  eventType: ActivityEventType;
  entityType: string;
  entityId: number;
  username: string;
  timestampUtc: string;
  description: string;
  loanId: number | null;
};

export type CommentResponse = {
  id: number;
  loanId: number;
  commentText: string;
  createdBy: string;
  createdAt: string;
};

export type AttachmentMetadata = {
  id: number;
  filename: string;
  fileSize: number;
  contentType: string;
  uploadedBy: string;
  uploadedAt: string;
};

export type BulkImportSummary = {
  totalRows: number;
  successCount: number;
  failureCount: number;
  importedStatementIds: number[];
  rowResults: Array<{
    rowNumber: number;
    status: "SUCCESS" | "FAILED";
    message: string;
    statementId: number | null;
  }>;
};

export type LoanImportBatchStatus = "PREVIEW_READY" | "COMPLETED" | "FAILED";
export type LoanImportRowAction = "CREATE" | "UPDATE" | "UNCHANGED" | "ERROR";

export type LoanImportBatch = {
  id: number;
  fileName: string;
  uploadedBy: string;
  startedAt: string;
  completedAt: string | null;
  status: LoanImportBatchStatus;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  createdCount: number;
  updatedCount: number;
  unchangedCount: number;
  failedCount: number;
  sourceSystem: string | null;
};

export type LoanImportRow = {
  id: number;
  rowNumber: number;
  sourceSystem: string | null;
  externalLoanId: string | null;
  borrowerName: string | null;
  action: LoanImportRowAction;
  validationMessage: string | null;
  loanId: number | null;
};

export type LoanImportPreviewResponse = {
  batch: LoanImportBatch;
  rows: LoanImportRow[];
};

export type LoanImportExecuteResponse = {
  batch: LoanImportBatch;
  rows: LoanImportRow[];
};

export type UserResponse = {
  id: number;
  username: string;
  email: string;
  active: boolean;
  roles: UserRole[];
  createdAt: string;
};

export type ProblemDetails = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  correlationId?: string;
  code?: string;
};

export type PageResponse<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
};

export type WebhookSubscription = {
  id: number;
  name: string;
  endpointUrl: string;
  eventFilters: string[];
  active: boolean;
  createdBy: string;
  createdAt: string;
};

export type WebhookDelivery = {
  id: number;
  eventOutboxId: number;
  eventId: string;
  subscriptionId: number;
  attemptNo: number;
  responseStatus: number | null;
  responseBodyHash: string | null;
  latencyMs: number | null;
  deliveryStatus: "SUCCESS" | "FAILED";
  errorCode: string | null;
  attemptedAt: string;
};

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
