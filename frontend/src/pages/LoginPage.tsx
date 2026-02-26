import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { BrandLogo } from "../components/BrandLogo";

export function LoginPage() {
  const [username, setUsername] = useState("analyst@demo.com");
  const [password, setPassword] = useState("Demo123!");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsLoading(true);
    try {
      await login(username, password);
      navigate("/app/dashboard");
    } catch (e) {
      const apiError = e as ApiError;
      const suffix = apiError.correlationId ? ` (Correlation: ${apiError.correlationId})` : "";
      setError(`${apiError.message}${suffix}`);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center p-6">
      <form onSubmit={onSubmit} className="card w-full max-w-sm p-7">
        <div className="mb-5">
          <BrandLogo size="sm" />
          <p className="mt-2 text-[10px] uppercase tracking-[0.16em] text-[var(--text-secondary)]">Secure Access</p>
        </div>
        <p className="text-xs text-[var(--text-secondary)]">
          Use seeded credentials. Analyst: `analyst@demo.com` / `Demo123!`
        </p>

        <label className="mt-5 block text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Username</label>
        <input className="input mt-2" value={username} onChange={(e) => setUsername(e.target.value)} />

        <label className="mt-3 block text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Password</label>
        <input
          type="password"
          className="input mt-2"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}

        <button type="submit" className="btn-primary mt-5 w-full" disabled={isLoading}>
          {isLoading ? "Signing in..." : "Sign In"}
        </button>
      </form>
    </div>
  );
}
