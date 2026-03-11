import type {
  ActivityLog,
  AttachmentMetadata,
  BulkImportSummary,
  CommentResponse,
  Covenant,
  CovenantResult,
  FinancialStatement,
  Loan,
  PageResponse,
  PortfolioSummary,
  RiskDetails,
  RiskSummary,
} from "../types/api";
import { makeQuery, request, requestRaw } from "./core/http";
import { getStoredSession } from "./core/session";

const BASE = "/api/v1";

export function getLoans(page = 0, size = 20, q = "") {
  return request<PageResponse<Loan>>(`/loans${makeQuery({ page, size, sort: "id,desc", q })}`);
}

export function getLoan(loanId: number) {
  return request<Loan>(`/loans/${loanId}`);
}

export function createLoan(payload: { borrowerName: string; principalAmount: number; startDate: string }) {
  return request<Loan>("/loans", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function closeLoan(loanId: number) {
  return request<Loan>(`/loans/${loanId}/close`, { method: "PATCH" });
}

export function addCovenant(
  loanId: number,
  payload: { type: string; thresholdValue: number; comparisonType: string; severityLevel: string }
) {
  return request<Covenant>(`/loans/${loanId}/covenants`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCovenants(loanId: number) {
  return request<Covenant[]>(`/loans/${loanId}/covenants`);
}

export function updateCovenant(
  loanId: number,
  covenantId: number,
  payload: { thresholdValue: number; comparisonType: string; severityLevel: string }
) {
  return request<Covenant>(`/loans/${loanId}/covenants/${covenantId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function getCovenantResults(loanId: number, page = 0, size = 20) {
  return request<PageResponse<CovenantResult>>(
    `/loans/${loanId}/covenant-results${makeQuery({ page, size, sort: "id,desc" })}`
  );
}

export function getRiskSummary(loanId: number) {
  return request<RiskSummary>(`/loans/${loanId}/risk-summary`);
}

export function getRiskDetails(loanId: number) {
  return request<RiskDetails>(`/loans/${loanId}/risk-details`);
}

export function submitStatement(
  loanId: number,
  payload: {
    periodType: string;
    fiscalYear: number;
    fiscalQuarter: number | null;
    currentAssets: number;
    currentLiabilities: number;
    totalDebt: number;
    totalEquity: number;
    ebit: number;
    interestExpense: number;
    netOperatingIncome?: number | null;
    totalDebtService?: number | null;
    intangibleAssets?: number | null;
    ebitda?: number | null;
    fixedCharges?: number | null;
    inventory?: number | null;
    totalAssets?: number | null;
    totalLiabilities?: number | null;
    submissionTimestampUtc?: string | null;
  }
) {
  return request<FinancialStatement>(`/loans/${loanId}/financial-statements`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function bulkImportStatements(loanId: number, file: File) {
  const body = new FormData();
  body.append("file", file);
  return request<BulkImportSummary>(`/loans/${loanId}/financial-statements/bulk-import`, {
    method: "POST",
    body,
  });
}

export function getComments(loanId: number, page = 0, size = 50) {
  return request<PageResponse<CommentResponse>>(
    `/loans/${loanId}/comments${makeQuery({ page, size, sort: "id,desc" })}`
  );
}

export function addComment(loanId: number, commentText: string) {
  return request<CommentResponse>(`/loans/${loanId}/comments`, {
    method: "POST",
    body: JSON.stringify({ commentText }),
  });
}

export function deleteComment(loanId: number, commentId: number) {
  return request<void>(`/loans/${loanId}/comments/${commentId}`, { method: "DELETE" });
}

export function getLoanActivity(loanId: number, page = 0, size = 25) {
  return request<PageResponse<ActivityLog>>(
    `/loans/${loanId}/activity${makeQuery({ page, size, sort: "timestampUtc,desc" })}`
  );
}

export function getPortfolioSummary() {
  return request<PortfolioSummary>("/portfolio/summary");
}

export function getAttachmentList(statementId: number) {
  return request<AttachmentMetadata[]>(`/financial-statements/${statementId}/attachments`);
}

export function uploadAttachment(statementId: number, file: File) {
  const body = new FormData();
  body.append("file", file);
  return request<AttachmentMetadata>(`/financial-statements/${statementId}/attachments`, {
    method: "POST",
    body,
  });
}

export function downloadAttachment(attachmentId: number) {
  const session = getStoredSession();
  const headers = new Headers();
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  return fetch(`${BASE}/attachments/${attachmentId}`, { headers });
}

export function deleteAttachment(attachmentId: number) {
  return request<void>(`/attachments/${attachmentId}`, { method: "DELETE" });
}

export function exportLoanAlerts(loanId: number) {
  return requestRaw(`/loans/${loanId}/alerts/export`);
}

export function exportLoanCovenantResults(loanId: number) {
  return requestRaw(`/loans/${loanId}/covenant-results/export`);
}
