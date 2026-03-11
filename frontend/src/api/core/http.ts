import type { AuthSession, ProblemDetails } from "../../types/api";
import { clearStoredSession, getStoredSession, setStoredSession } from "./session";

const BASE = "/api/v1";

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

export function makeQuery(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const built = query.toString();
  return built.length ? `?${built}` : "";
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

function toApiError(response: Response, details: ProblemDetails | null) {
  return new ApiError(
    details?.detail ?? details?.title ?? `HTTP ${response.status}`,
    response.status,
    details?.correlationId
  );
}

export async function request<T>(path: string, init?: RequestInit, retry = true): Promise<T> {
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
    throw toApiError(response, details);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

export async function requestRaw(path: string, init?: AuthenticatedRawRequest, retry = true): Promise<Response> {
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
    throw toApiError(response, details);
  }

  return response;
}

export async function requestPublic<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, init);
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
  return response.json() as Promise<T>;
}
