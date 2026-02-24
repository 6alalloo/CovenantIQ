import type {
  Alert,
  Covenant,
  CovenantResult,
  Loan,
  PageResponse,
  RiskSummary,
} from "../types/api";

const BASE = "/api/v1";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try {
      const body = await response.json();
      message = body.detail ?? body.title ?? message;
    } catch {
      // fallback
    }
    throw new Error(message);
  }
  if (response.status === 204) {
    return {} as T;
  }
  return response.json() as Promise<T>;
}

export function createLoan(payload: {
  borrowerName: string;
  principalAmount: number;
  startDate: string;
}) {
  return request<Loan>("/loans", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getLoans(page = 0, size = 20) {
  return request<PageResponse<Loan>>(`/loans?page=${page}&size=${size}&sort=id,desc`);
}

export function addCovenant(
  loanId: number,
  payload: {
    type: string;
    thresholdValue: number;
    comparisonType: string;
    severityLevel: string;
  }
) {
  return request<Covenant>(`/loans/${loanId}/covenants`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
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
  }
) {
  return request(`/loans/${loanId}/financial-statements`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCovenantResults(loanId: number, page = 0, size = 20) {
  return request<PageResponse<CovenantResult>>(
    `/loans/${loanId}/covenant-results?page=${page}&size=${size}&sort=id,desc`
  );
}

export function getAlerts(loanId: number, page = 0, size = 20) {
  return request<PageResponse<Alert>>(`/loans/${loanId}/alerts?page=${page}&size=${size}&sort=id,desc`);
}

export function getRiskSummary(loanId: number) {
  return request<RiskSummary>(`/loans/${loanId}/risk-summary`);
}

export function closeLoan(loanId: number) {
  return request<Loan>(`/loans/${loanId}/close`, { method: "PATCH" });
}
