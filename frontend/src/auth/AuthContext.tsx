import { createContext, useContext, useMemo, useState } from "react";

type AuthContextType = {
  isAuthenticated: boolean;
  userName: string | null;
  login: (name: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [userName, setUserName] = useState<string | null>(localStorage.getItem("covenantiq_user"));

  const value = useMemo<AuthContextType>(
    () => ({
      isAuthenticated: !!userName,
      userName,
      login: (name: string) => {
        localStorage.setItem("covenantiq_user", name);
        setUserName(name);
      },
      logout: () => {
        localStorage.removeItem("covenantiq_user");
        setUserName(null);
      },
    }),
    [userName]
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
