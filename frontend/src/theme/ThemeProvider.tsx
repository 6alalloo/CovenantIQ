import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { THEME_STORAGE_KEY, resolveTheme } from "./theme";
import type { ResolvedTheme, ThemeContextValue, ThemeMode } from "./theme";

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

function getInitialMode(): ThemeMode {
  const saved = localStorage.getItem(THEME_STORAGE_KEY);
  if (saved === "dark" || saved === "light" || saved === "system") {
    return saved;
  }
  return "dark";
}

function getSystemPrefersDark() {
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(() => getInitialMode());
  const [prefersDark, setPrefersDark] = useState<boolean>(() => getSystemPrefersDark());

  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = (event: MediaQueryListEvent) => setPrefersDark(event.matches);
    media.addEventListener("change", handler);
    return () => media.removeEventListener("change", handler);
  }, []);

  const resolvedMode = useMemo<ResolvedTheme>(() => resolveTheme(mode, prefersDark), [mode, prefersDark]);

  useEffect(() => {
    localStorage.setItem(THEME_STORAGE_KEY, mode);
  }, [mode]);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", resolvedMode);
  }, [resolvedMode]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      mode,
      resolvedMode,
      setMode,
    }),
    [mode, resolvedMode]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useThemeContext() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used within ThemeProvider");
  }
  return context;
}
