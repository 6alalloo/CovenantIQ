import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { BrandLogo } from "../components/BrandLogo";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";

export function LoginPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const defaultCredentials = useMemo(
    () => ({
      username: sampleUxEnabled ? "analyst@demo.com" : "",
      password: sampleUxEnabled ? "Demo123!" : "",
    }),
    [sampleUxEnabled]
  );
  const [username, setUsername] = useState(defaultCredentials.username);
  const [password, setPassword] = useState(defaultCredentials.password);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (sampleUxEnabled && !username && !password) {
      setUsername(defaultCredentials.username);
      setPassword(defaultCredentials.password);
    }
  }, [defaultCredentials.password, defaultCredentials.username, password, sampleUxEnabled, username]);

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
      <form onSubmit={onSubmit} className="card w-full max-w-md p-8" data-testid="login-form">
        <div className="mb-6">
          <BrandLogo size="sm" />
          <p className="mt-2 text-[10px] uppercase tracking-[0.16em] text-[var(--text-secondary)]">Secure Access</p>
        </div>
        {sampleUxEnabled ? (
          <p className="text-sm text-[var(--text-secondary)]">
            Sample UX is enabled. Seeded demo credentials are prefilled for local demonstration.
          </p>
        ) : (
          <p className="text-sm text-[var(--text-secondary)]">
            Sign in with an explicitly provisioned account.
          </p>
        )}

        <label className="mt-5 block text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Username</label>
        <input
          className="input mt-2"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          data-testid="login-username"
        />

        <label className="mt-3 block text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Password</label>
        <input
          type="password"
          className="input mt-2"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          data-testid="login-password"
        />

        {error ? (
          <p className="mt-4 text-sm text-[var(--risk-high)]" data-testid="login-error">
            {error}
          </p>
        ) : null}

        <button type="submit" className="btn-primary mt-5 w-full" disabled={isLoading} data-testid="login-submit">
          {isLoading ? "Signing in..." : "Sign In"}
        </button>
      </form>
    </div>
  );
}
