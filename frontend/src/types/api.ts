export type LoanStatus = "ACTIVE" | "CLOSED";
export type CovenantType = "CURRENT_RATIO" | "DEBT_TO_EQUITY";
export type ComparisonType = "GREATER_THAN_EQUAL" | "LESS_THAN_EQUAL";
export type SeverityLevel = "LOW" | "MEDIUM" | "HIGH";
export type PeriodType = "QUARTERLY" | "ANNUAL";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type CovenantResultStatus = "PASS" | "BREACH";
export type AlertType = "BREACH" | "EARLY_WARNING";

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
};

export type RiskSummary = {
  totalCovenants: number;
  breachedCount: number;
  activeWarnings: number;
  overallRiskLevel: RiskLevel;
};

export type PageResponse<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
};
