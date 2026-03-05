import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getRuntimeConfig } from "../api/client";
import type { RuntimeConfig } from "../types/api";

const SAMPLE_UX_STORAGE_KEY = "covenantiq_sample_ux_enabled";

const DEFAULT_RUNTIME_CONFIG: RuntimeConfig = {
  backendMode: "NORMAL",
  demoMode: false,
  testMode: false,
  sampleContentAvailable: false,
  strictSecretValidationEnabled: true,
};

type RuntimeConfigContextType = {
  runtimeConfig: RuntimeConfig;
  sampleUxEnabled: boolean;
  setSampleUxEnabled: (enabled: boolean) => void;
  isLoaded: boolean;
};

const RuntimeConfigContext = createContext<RuntimeConfigContextType | undefined>(undefined);

export function RuntimeConfigProvider({ children }: { children: React.ReactNode }) {
  const [runtimeConfig, setRuntimeConfig] = useState<RuntimeConfig>(DEFAULT_RUNTIME_CONFIG);
  const [sampleUxEnabled, setSampleUxEnabledState] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const next = await getRuntimeConfig();
        if (cancelled) return;
        setRuntimeConfig(next);
        const saved = localStorage.getItem(SAMPLE_UX_STORAGE_KEY) === "true";
        setSampleUxEnabledState(next.sampleContentAvailable && saved);
      } catch {
        if (cancelled) return;
        setRuntimeConfig(DEFAULT_RUNTIME_CONFIG);
        setSampleUxEnabledState(false);
      } finally {
        if (!cancelled) {
          setIsLoaded(true);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (runtimeConfig.sampleContentAvailable) {
      return;
    }
    localStorage.removeItem(SAMPLE_UX_STORAGE_KEY);
    setSampleUxEnabledState(false);
  }, [runtimeConfig.sampleContentAvailable]);

  const value = useMemo<RuntimeConfigContextType>(
    () => ({
      runtimeConfig,
      sampleUxEnabled,
      isLoaded,
      setSampleUxEnabled: (enabled: boolean) => {
        const next = runtimeConfig.sampleContentAvailable && enabled;
        setSampleUxEnabledState(next);
        if (next) {
          localStorage.setItem(SAMPLE_UX_STORAGE_KEY, "true");
        } else {
          localStorage.removeItem(SAMPLE_UX_STORAGE_KEY);
        }
      },
    }),
    [isLoaded, runtimeConfig, sampleUxEnabled]
  );

  return <RuntimeConfigContext.Provider value={value}>{children}</RuntimeConfigContext.Provider>;
}

export function useRuntimeConfig() {
  const context = useContext(RuntimeConfigContext);
  if (!context) {
    throw new Error("useRuntimeConfig must be used within RuntimeConfigProvider");
  }
  return context;
}
