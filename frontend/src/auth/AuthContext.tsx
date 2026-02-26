import { createContext, useContext, useMemo, useState } from "react";
import { clearStoredSession, getStoredSession, login as apiLogin } from "../api/client";
import type { AuthSession, UserRole } from "../types/api";

type AuthContextType = {
  isAuthenticated: boolean;
  session: AuthSession | null;
  userName: string | null;
  roles: UserRole[];
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasAnyRole: (allowed: UserRole[]) => boolean;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(getStoredSession());

  const value = useMemo<AuthContextType>(
    () => ({
      isAuthenticated: !!session?.accessToken,
      session,
      userName: session?.username ?? null,
      roles: session?.roles ?? [],
      login: async (username: string, password: string) => {
        const next = await apiLogin(username, password);
        setSession(next);
      },
      logout: () => {
        clearStoredSession();
        setSession(null);
      },
      hasAnyRole: (allowed: UserRole[]) => !!session?.roles.some((role) => allowed.includes(role)),
    }),
    [session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
