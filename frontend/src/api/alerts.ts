import type { Alert, AlertStatus, PageResponse } from "../types/api";
import { makeQuery, request } from "./core/http";
import { getLoans } from "./loans";

export function getAlertsForLoan(loanId: number, page = 0, size = 20) {
  return request<PageResponse<Alert>>(`/loans/${loanId}/alerts${makeQuery({ page, size, sort: "id,desc" })}`);
}

export async function getAlertsGlobal() {
  const loans = await getLoans(0, 100);
  const pages = await Promise.all(loans.content.map((loan) => getAlertsForLoan(loan.id, 0, 50)));
  return pages.flatMap((page) => page.content);
}

export function updateAlertStatus(alertId: number, status: AlertStatus, resolutionNotes?: string) {
  return request<Alert>(`/alerts/${alertId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, resolutionNotes: resolutionNotes ?? null }),
  });
}
