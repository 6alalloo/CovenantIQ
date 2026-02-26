import { FormEvent, useEffect, useState } from "react";
import { createUser, deactivateUser, getUsers, updateUserRoles } from "../api/client";
import type { UserResponse, UserRole } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

const ROLE_OPTIONS: UserRole[] = ["ANALYST", "RISK_LEAD", "ADMIN"];

export function AdminUsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    username: "",
    email: "",
    password: "",
    roles: ["ANALYST"] as UserRole[],
  });

  const load = async () => {
    try {
      const page = await getUsers();
      setUsers(page.content);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createUser(form);
      setForm({ username: "", email: "", password: "", roles: ["ANALYST"] });
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const onToggleRole = async (user: UserResponse, role: UserRole) => {
    const nextRoles = user.roles.includes(role) ? user.roles.filter((item) => item !== role) : [...user.roles, role];
    if (!nextRoles.length) return;
    try {
      await updateUserRoles(user.id, nextRoles);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const onDeactivate = async (id: number) => {
    try {
      await deactivateUser(id);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <PageSection title="User Management" subtitle="Admin-only user lifecycle: create, role edit, and deactivate.">
      <div className="grid gap-3 xl:grid-cols-[1.3fr_1fr]">
        <Surface className="p-4">
          <h2 className="panel-title">Users</h2>
          <table className="table-base mt-2">
            <thead>
              <tr>
                <th>User</th>
                <th>Roles</th>
                <th>Active</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <p className="font-semibold">{user.username}</p>
                    <p className="text-xs text-[var(--text-secondary)]">{user.email}</p>
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      {ROLE_OPTIONS.map((role) => (
                        <button
                          key={role}
                          className={`rounded-sm border px-2 py-1 text-[11px] uppercase tracking-[0.06em] ${
                            user.roles.includes(role)
                              ? "border-[var(--accent-interactive)] bg-[color:rgb(142_184_255_/_0.13)] text-[var(--text-primary)]"
                              : "border-[var(--border-default)] bg-[#0e0e0e] text-[var(--text-secondary)]"
                          }`}
                          onClick={() => void onToggleRole(user, role)}
                        >
                          {role}
                        </button>
                      ))}
                    </div>
                  </td>
                  <td>{user.active ? "YES" : "NO"}</td>
                  <td className="text-right">
                    {user.active ? (
                      <Button variant="outline" onClick={() => void onDeactivate(user.id)}>
                        Deactivate
                      </Button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Surface>

        <Surface className="p-4">
          <h2 className="panel-title">Create User</h2>
          <form className="mt-3 space-y-3" onSubmit={onCreate}>
            <Input
              placeholder="Username"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              required
            />
            <Input
              placeholder="Email"
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              required
            />
            <Input
              placeholder="Password"
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              required
            />
            <label className="text-xs uppercase tracking-wide text-[var(--text-secondary)]">Roles</label>
            <div className="flex flex-wrap gap-2">
              {ROLE_OPTIONS.map((role) => (
                <label key={role} className="badge cursor-pointer">
                  <input
                    className="mr-1"
                    type="checkbox"
                    checked={form.roles.includes(role)}
                    onChange={(event) =>
                      setForm((prev) => ({
                        ...prev,
                        roles: event.target.checked
                          ? [...prev.roles, role]
                          : prev.roles.filter((existing) => existing !== role),
                      }))
                    }
                  />
                  {role}
                </label>
              ))}
            </div>
            <Button className="w-full" type="submit">
              Create User
            </Button>
          </form>
        </Surface>
      </div>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
