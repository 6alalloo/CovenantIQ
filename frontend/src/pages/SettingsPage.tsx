import { FormEvent, useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Select } from "../components/ui/select";
import { formatDateTime, formatEnumLabel } from "../lib/format";
import type { ThemeMode } from "../theme/theme";
import { useTheme } from "../theme/useTheme";

export function SettingsPage() {
  const { userName, roles } = useAuth();
  const { mode, setMode, resolvedMode } = useTheme();
  const [draftMode, setDraftMode] = useState<ThemeMode>(mode);
  const [savedMode, setSavedMode] = useState<ThemeMode>(mode);
  const [savedAt, setSavedAt] = useState<string | null>(null);

  useEffect(() => {
    setDraftMode(mode);
    setSavedMode(mode);
  }, [mode]);

  const isDirty = useMemo(() => draftMode !== savedMode, [draftMode, savedMode]);

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
    setSavedAt(new Date().toISOString());
  };

  return (
    <PageSection title="Settings" subtitle="Control active account context and appearance preferences.">
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
            <h2 className="panel-title">Status</h2>
            <div className="mt-3 grid gap-3 md:grid-cols-3">
              <div className="rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
                <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">Pending Changes</p>
                <p className="mt-1 text-sm">{isDirty ? "Unsaved appearance change" : "No pending changes"}</p>
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
