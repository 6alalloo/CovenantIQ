import type { PageResponse, UserResponse, UserRole } from "../types/api";
import { makeQuery, request } from "./core/http";

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
