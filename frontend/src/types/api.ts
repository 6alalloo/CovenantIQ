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
  | "COVENANT_CREATED"
  | "STATEMENT_SUBMITTED"
  | "ALERT_ACKNOWLEDGED"
  | "ALERT_RESOLVED"
  | "COMMENT_ADDED"
  | "COMMENT_DELETED"
  | "USER_CREATED"
  | "USER_UPDATED"
  | "USER_DEACTIVATED";

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
};

export type PageResponse<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
};
