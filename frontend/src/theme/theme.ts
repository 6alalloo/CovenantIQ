export type ThemeMode = "dark" | "light" | "system";
export type ResolvedTheme = "dark" | "light";

export type ThemeContextValue = {
  mode: ThemeMode;
  resolvedMode: ResolvedTheme;
  setMode: (mode: ThemeMode) => void;
};

export const THEME_STORAGE_KEY = "covenantiq.theme_mode";

export function resolveTheme(mode: ThemeMode, prefersDark: boolean): ResolvedTheme {
  if (mode === "system") {
    return prefersDark ? "dark" : "light";
  }
  return mode;
}

export type ChartToken = "neutral1" | "neutral2" | "accent" | "high" | "medium" | "low";

export const chartTokenValue: Record<ChartToken, string> = {
  neutral1: "var(--chart-neutral-1)",
  neutral2: "var(--chart-neutral-2)",
  accent: "var(--chart-accent)",
  high: "var(--risk-high)",
  medium: "var(--risk-medium)",
  low: "var(--risk-low)",
};
