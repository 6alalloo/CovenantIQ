import type {
  ActivityLog,
  Alert,
  AlertStatus,
  AttachmentMetadata,
  AuthSession,
  BulkImportSummary,
  ChangeRequest,
  CommentResponse,
  CollateralAsset,
  Covenant,
  CovenantException,
  CovenantResult,
  FinancialStatement,
  Loan,
  LoanImportBatch,
  LoanImportExecuteResponse,
  LoanImportPreviewResponse,
  LoanImportRow,
  PageResponse,
  PortfolioSummary,
  ProblemDetails,
  ReleaseBatch,
  RiskDetails,
  RiskSummary,
  RuntimeConfig,
  Ruleset,
  RulesetValidationResult,
  RulesetVersion,
  UserResponse,
  UserRole,
  WebhookDelivery,
  WebhookSubscription,
  WorkflowDefinition,
  WorkflowInstance,
} from "../types/api";

const BASE = "/api/v1";
const SESSION_KEY = "covenantiq_session";

export class ApiError extends Error {
  status: number;
  correlationId?: string;

  constructor(message: string, status: number, correlationId?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.correlationId = correlationId;
  }
}

type AuthenticatedRawRequest = RequestInit & {
  headers?: HeadersInit;
};

function makeQuery(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const built = query.toString();
  return built.length ? `?${built}` : "";
}

export function getStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function setStoredSession(session: AuthSession) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export async function getRuntimeConfig() {
  const response = await fetch(`${BASE}/runtime-config`);
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
  return response.json() as Promise<RuntimeConfig>;
}
export function clearStoredSession() {
  localStorage.removeItem(SESSION_KEY);
}

async function refreshSessionToken(refreshToken: string): Promise<AuthSession | null> {
  const response = await fetch(`${BASE}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    return null;
  }
  const next = (await response.json()) as AuthSession;
  setStoredSession(next);
  return next;
}

async function request<T>(path: string, init?: RequestInit, retry = true): Promise<T> {
  const currentSession = getStoredSession();
  const isMultipart = init?.body instanceof FormData;
  const headers = new Headers(init?.headers);
  if (!isMultipart && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (currentSession?.accessToken) {
    headers.set("Authorization", `Bearer ${currentSession.accessToken}`);
  }

  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && retry && currentSession?.refreshToken && !path.startsWith("/auth/")) {
    const nextSession = await refreshSessionToken(currentSession.refreshToken);
    if (nextSession) {
      return request<T>(path, init, false);
    }
    clearStoredSession();
  }

  if (!response.ok) {
    let details: ProblemDetails | null = null;
    try {
      details = (await response.json()) as ProblemDetails;
    } catch {
      details = null;
    }
    throw new ApiError(
      details?.detail ?? details?.title ?? `HTTP ${response.status}`,
      response.status,
      details?.correlationId
    );
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

async function requestRaw(path: string, init?: AuthenticatedRawRequest, retry = true): Promise<Response> {
  const currentSession = getStoredSession();
  const headers = new Headers(init?.headers);
  if (currentSession?.accessToken) {
    headers.set("Authorization", `Bearer ${currentSession.accessToken}`);
  }

  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && retry && currentSession?.refreshToken && !path.startsWith("/auth/")) {
    const nextSession = await refreshSessionToken(currentSession.refreshToken);
    if (nextSession) {
      return requestRaw(path, init, false);
    }
    clearStoredSession();
  }

  if (!response.ok) {
    let details: ProblemDetails | null = null;
    try {
      details = (await response.json()) as ProblemDetails;
    } catch {
      details = null;
    }
    throw new ApiError(
      details?.detail ?? details?.title ?? `HTTP ${response.status}`,
      response.status,
      details?.correlationId
    );
  }

  return response;
}

export async function login(username: string, password: string) {
  const session = await request<AuthSession>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  setStoredSession(session);
  return session;
}

export function refresh(refreshToken: string) {
  return request<AuthSession>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
}

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

export function getAlertsForLoan(loanId: number, page = 0, size = 20) {
  return request<PageResponse<Alert>>(`/loans/${loanId}/alerts${makeQuery({ page, size, sort: "id,desc" })}`);
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

export function getUsers(page = 0, size = 20) {
  return request<PageResponse<UserResponse>>(`/users${makeQuery({ page, size, sort: "id,desc" })}`);
}

export function createUser(payload: { username: string; email: string; password: string; roles: UserRole[] }) {
  return request<UserResponse>("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateUserRoles(userId: number, roles: UserRole[]) {
  return request<UserResponse>(`/users/${userId}/roles`, {
    method: "PATCH",
    body: JSON.stringify({ roles }),
  });
}

export function deactivateUser(userId: number) {
  return request<void>(`/users/${userId}`, { method: "DELETE" });
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

export function createWebhookSubscription(payload: {
  name: string;
  endpointUrl: string;
  secret: string;
  eventFilters: string[];
}) {
  return request<WebhookSubscription>("/integrations/webhooks", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getWebhookSubscriptions() {
  return request<WebhookSubscription[]>("/integrations/webhooks");
}

export function updateWebhookSubscription(
  id: number,
  payload: Partial<{ name: string; endpointUrl: string; secret: string; eventFilters: string[]; active: boolean }>
) {
  return request<WebhookSubscription>(`/integrations/webhooks/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function getWebhookDeliveries(id: number) {
  return request<WebhookDelivery[]>(`/integrations/webhooks/${id}/deliveries`);
}

export function retryWebhookOutboxEvent(eventOutboxId: number) {
  return request<void>(`/integrations/webhooks/deliveries/${eventOutboxId}/retry`, { method: "POST" });
}

export function createWorkflowDefinition(payload: {
  entityType: string;
  name: string;
  states: Array<{ stateCode: string; initial: boolean; terminal: boolean }>;
  transitions: Array<{
    fromState: string;
    toState: string;
    allowedRoles: string[];
    requiredFields: string[];
    guardExpression?: string;
  }>;
}) {
  return request<WorkflowDefinition>("/workflows/definitions", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getWorkflowDefinitions(entityType?: string) {
  return request<WorkflowDefinition[]>(`/workflows/definitions${makeQuery({ entityType })}`);
}

export function publishWorkflowDefinition(id: number, reason: string) {
  return request<WorkflowDefinition>(`/workflows/definitions/${id}/publish`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getWorkflowInstance(entityType: string, entityId: number) {
  return request<WorkflowInstance>(`/workflows/instances/${entityType}/${entityId}`);
}

export function transitionWorkflowInstance(id: number, toState: string, reason: string, metadata?: Record<string, unknown>) {
  return request<WorkflowInstance>(`/workflows/instances/${id}/transition`, {
    method: "POST",
    body: JSON.stringify({ toState, reason, metadata: metadata ?? {} }),
  });
}

export function createRuleset(payload: { key: string; name: string; domain: "COVENANT_EVAL"; ownerRole: string }) {
  return request<Ruleset>("/rulesets", { method: "POST", body: JSON.stringify(payload) });
}

export function getRulesets() {
  return request<Ruleset[]>("/rulesets");
}

export function createRulesetVersion(
  rulesetId: number,
  payload: { definitionJson: string; schemaVersion: number; changeSummary?: string }
) {
  return request<RulesetVersion>(`/rulesets/${rulesetId}/versions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function validateRulesetVersion(
  rulesetId: number,
  version: number,
  payload: { input?: Record<string, unknown>; expectedOutput?: Record<string, unknown> } = {}
) {
  return request<RulesetValidationResult>(`/rulesets/${rulesetId}/versions/${version}/validate`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function publishRulesetVersion(rulesetId: number, version: number, reason: string) {
  return request<RulesetVersion>(`/rulesets/${rulesetId}/versions/${version}/publish`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getRulesetVersions(rulesetId: number) {
  return request<RulesetVersion[]>(`/rulesets/${rulesetId}/versions`);
}

export function createCollateral(
  loanId: number,
  payload: {
    assetType: string;
    description?: string;
    nominalValue: number;
    haircutPct: number;
    lienRank: number;
    currency: string;
    effectiveDate: string;
  }
) {
  return request<CollateralAsset>(`/loans/${loanId}/collaterals`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCollaterals(loanId: number) {
  return request<CollateralAsset[]>(`/loans/${loanId}/collaterals`);
}

export function createCovenantException(
  loanId: number,
  payload: {
    covenantId: number;
    exceptionType: "WAIVER" | "OVERRIDE";
    reason: string;
    effectiveFrom: string;
    effectiveTo: string;
    controlsJson?: string;
  }
) {
  return request<CovenantException>(`/loans/${loanId}/exceptions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCovenantExceptions(loanId: number) {
  return request<CovenantException[]>(`/loans/${loanId}/exceptions`);
}

export function approveException(id: number) {
  return request<CovenantException>(`/exceptions/${id}/approve`, { method: "PATCH" });
}

export function expireException(id: number) {
  return request<CovenantException>(`/exceptions/${id}/expire`, { method: "PATCH" });
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
  return request<ChangeRequest>("/change-requests", { method: "POST", body: JSON.stringify(payload) });
}

export function approveChangeRequest(id: number) {
  return request<ChangeRequest>(`/change-requests/${id}/approve`, { method: "PATCH" });
}

export function getChangeRequests() {
  return request<ChangeRequest[]>("/change-requests");
}

export function createRelease(payload: { changeRequestId: number; releaseTag: string }) {
  return request<ReleaseBatch>("/releases", { method: "POST", body: JSON.stringify(payload) });
}

export function rollbackRelease(id: number, payload: { targetReleaseId: number; justification: string }) {
  return request<ReleaseBatch>(`/releases/${id}/rollback`, { method: "POST", body: JSON.stringify(payload) });
}

export function getReleases() {
  return request<ReleaseBatch[]>("/releases");
}


export function previewLoanImport(file: File) {
  const body = new FormData();
  body.append("file", file);
  return request<LoanImportPreviewResponse>("/admin/loan-imports/preview", {
    method: "POST",
    body,
  });
}

export function runLoanImport(batchId: number) {
  return request<LoanImportExecuteResponse>(`/admin/loan-imports/${batchId}/execute`, {
    method: "POST",
  });
}

export function getLoanImports() {
  return request<LoanImportBatch[]>("/admin/loan-imports");
}

export function getLoanImport(batchId: number) {
  return request<LoanImportBatch>(`/admin/loan-imports/${batchId}`);
}

export function getLoanImportRows(batchId: number) {
  return request<LoanImportRow[]>(`/admin/loan-imports/${batchId}/rows`);
}
