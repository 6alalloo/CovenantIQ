import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const DEMO_USERS = ["Analyst.A", "Analyst.B", "Risk.Lead"];

export function LoginPage() {
  const [selectedUser, setSelectedUser] = useState(DEMO_USERS[0]);
  const { login } = useAuth();
  const navigate = useNavigate();

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    login(selectedUser);
    navigate("/app");
  }

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-2xl bg-white shadow-panel p-8">
        <h1 className="text-3xl font-semibold text-ink">CovenantIQ</h1>
        <p className="text-slate-600 mt-2">Mock analyst login for demo mode</p>
        <label className="block mt-6 text-sm font-medium text-slate-700">Select Demo User</label>
        <select
          value={selectedUser}
          onChange={(e) => setSelectedUser(e.target.value)}
          className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2"
        >
          {DEMO_USERS.map((user) => (
            <option key={user} value={user}>
              {user}
            </option>
          ))}
        </select>
        <button
          type="submit"
          className="mt-6 w-full rounded-lg bg-primary px-4 py-2 font-semibold text-white hover:opacity-95"
        >
          Enter Dashboard
        </button>
      </form>
    </div>
  );
}
