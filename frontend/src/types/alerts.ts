import type { SeverityLevel } from "./loans";

export type AlertType = "BREACH" | "EARLY_WARNING";
export type AlertStatus = "OPEN" | "ACKNOWLEDGED" | "UNDER_REVIEW" | "RESOLVED";

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
