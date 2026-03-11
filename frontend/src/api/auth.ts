import type { AuthSession } from "../types/api";
import { request } from "./core/http";
import { setStoredSession } from "./core/session";

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
