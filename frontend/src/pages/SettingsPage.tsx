import { FormEvent, useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Select } from "../components/ui/select";
import { Switch } from "../components/ui/switch";
import { formatDateTime, formatEnumLabel } from "../lib/format";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";
import type { ThemeMode } from "../theme/theme";
import { useTheme } from "../theme/useTheme";

export function SettingsPage() {
  const { userName, roles } = useAuth();
  const { mode, setMode, resolvedMode } = useTheme();
  const { runtimeConfig, sampleUxEnabled, setSampleUxEnabled } = useRuntimeConfig();
  const [draftMode, setDraftMode] = useState<ThemeMode>(mode);
  const [savedMode, setSavedMode] = useState<ThemeMode>(mode);
  const [draftSampleUxEnabled, setDraftSampleUxEnabled] = useState(sampleUxEnabled);
  const [savedSampleUxEnabled, setSavedSampleUxEnabled] = useState(sampleUxEnabled);
  const [savedAt, setSavedAt] = useState<string | null>(null);

  useEffect(() => {
    setDraftMode(mode);
    setSavedMode(mode);
  }, [mode]);

  useEffect(() => {
    setDraftSampleUxEnabled(sampleUxEnabled);
    setSavedSampleUxEnabled(sampleUxEnabled);
  }, [sampleUxEnabled]);

  const isDirty = useMemo(
    () => draftMode !== savedMode || draftSampleUxEnabled !== savedSampleUxEnabled,
    [draftMode, draftSampleUxEnabled, savedMode, savedSampleUxEnabled]
  );

  useEffect(() => {
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!isDirty) return;
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [isDirty]);

  const onSave = (event: FormEvent) => {
    event.preventDefault();
    setMode(draftMode);
    setSavedMode(draftMode);
    setSampleUxEnabled(draftSampleUxEnabled);
    setSavedSampleUxEnabled(draftSampleUxEnabled && runtimeConfig.sampleContentAvailable);
    setSavedAt(new Date().toISOString());
  };

  return (
    <PageSection title="Settings" subtitle="Control active account context, appearance preferences, and sample UX behavior.">
      <form className="space-y-3" onSubmit={onSave}>
        <div className="grid gap-3 xl:grid-cols-3">
          <Surface className="p-5">
            <h2 className="panel-title">Profile & Access</h2>
            <div className="mt-3 space-y-2 text-sm">
              <p>
                <span className="text-[var(--text-secondary)]">Username:</span> {userName}
              </p>
              <p>
                <span className="text-[var(--text-secondary)]">Roles:</span> {roles.map((role) => formatEnumLabel(role)).join(", ")}
              </p>
              <p>
                <span className="text-[var(--text-secondary)]">Active theme:</span> {formatEnumLabel(resolvedMode)}
              </p>
              <p>
                <span className="text-[var(--text-secondary)]">Backend mode:</span> {formatEnumLabel(runtimeConfig.backendMode)}
              </p>
            </div>
          </Surface>

          <Surface className="p-5 xl:col-span-2">
            <h2 className="panel-title">Appearance</h2>
            <p className="mt-1 text-xs text-[var(--text-secondary)]">Changes apply after you save.</p>
            <div className="mt-3 grid gap-3 rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] px-3 py-3 md:grid-cols-[240px_1fr] md:items-start">
              <div>
                <p className="text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Theme Mode</p>
                <Select className="mt-2" value={draftMode} onChange={(event) => setDraftMode(event.target.value as ThemeMode)}>
                  <option value="system">System</option>
                  <option value="dark">Dark</option>
                  <option value="light">Light</option>
                </Select>
              </div>
              <p className="text-sm text-[var(--text-secondary)]">
                Choose the theme source used across the application shell. `System` follows your OS preference,
                while `Dark` and `Light` pin the mode explicitly.
              </p>
            </div>
          </Surface>

          <Surface className="p-5 xl:col-span-3">
            <h2 className="panel-title">Environment Controls</h2>
            <div className="mt-3 grid gap-3 xl:grid-cols-[1.2fr_1fr]">
              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Sample UX</p>
                    <p className="mt-2 text-sm text-[var(--text-secondary)]">
                      Controls frontend-only sample content such as prefilled forms and illustrative charts. Backend startup mode remains config-driven and requires restart to change.
                    </p>
                  </div>
                  <Switch
                    checked={draftSampleUxEnabled && runtimeConfig.sampleContentAvailable}
                    onCheckedChange={(checked) => setDraftSampleUxEnabled(runtimeConfig.sampleContentAvailable && checked)}
                    className={!runtimeConfig.sampleContentAvailable ? "opacity-50" : ""}
                    disabled={!runtimeConfig.sampleContentAvailable}
                  />
                </div>
                {!runtimeConfig.sampleContentAvailable ? (
                  <p className="mt-3 text-xs text-[var(--risk-medium)]">
                    Sample UX is unavailable while backend mode is `NORMAL`.
                  </p>
                ) : null}
              </div>

              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-4">
                <p className="text-xs uppercase tracking-[0.08em] text-[var(--text-secondary)]">Backend Runtime</p>
                <div className="mt-3 space-y-2 text-sm text-[var(--text-secondary)]">
                  <p>Mode: {formatEnumLabel(runtimeConfig.backendMode)}</p>
                  <p>Demo mode: {runtimeConfig.demoMode ? "Enabled" : "Disabled"}</p>
                  <p>Test mode: {runtimeConfig.testMode ? "Enabled" : "Disabled"}</p>
                  <p>Strict secret validation: {runtimeConfig.strictSecretValidationEnabled ? "Enabled" : "Disabled"}</p>
                </div>
              </div>
            </div>
          </Surface>

          <Surface className="p-5 xl:col-span-3">
            <h2 className="panel-title">Status</h2>
            <div className="mt-3 grid gap-3 md:grid-cols-3">
              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
                <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">Pending Changes</p>
                <p className="mt-1 text-sm">{isDirty ? "Unsaved settings change" : "No pending changes"}</p>
              </div>
              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
                <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">Current Theme</p>
                <p className="mt-1 text-sm">{formatEnumLabel(resolvedMode)}</p>
              </div>
              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
                <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">Last Saved</p>
                <p className="mt-1 text-sm">{savedAt ? formatDateTime(savedAt) : "Not saved in this session"}</p>
              </div>
            </div>
          </Surface>
        </div>

        <Surface className="p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {isDirty ? "You have unsaved changes." : "All changes are saved."}
            </p>
            <Button type="submit" disabled={!isDirty}>
              Save Changes
            </Button>
          </div>
          {savedAt ? <p className="mt-3 text-xs text-[var(--risk-low)]">Saved at {formatDateTime(savedAt)}</p> : null}
        </Surface>
      </form>
    </PageSection>
  );
}


