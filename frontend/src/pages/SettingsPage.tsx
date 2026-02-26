import { FormEvent, useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { PageSection, Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Switch } from "../components/ui/switch";

type Preferences = {
  emailAlerts: boolean;
  inAppAlerts: boolean;
  reducedMotion: boolean;
};

const PREF_KEY = "covenantiq_preferences";

export function SettingsPage() {
  const { userName, roles } = useAuth();
  const [prefs, setPrefs] = useState<Preferences>({
    emailAlerts: true,
    inAppAlerts: true,
    reducedMotion: false,
  });
  const [savedAt, setSavedAt] = useState<string | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem(PREF_KEY);
    if (raw) {
      try {
        setPrefs(JSON.parse(raw) as Preferences);
      } catch {
        // ignore malformed prefs
      }
    }
  }, []);

  const onSave = (event: FormEvent) => {
    event.preventDefault();
    localStorage.setItem(PREF_KEY, JSON.stringify(prefs));
    setSavedAt(new Date().toISOString());
  };

  return (
    <PageSection title="Settings" subtitle="Operational MVP: identity from session and local preference persistence.">
      <div className="grid gap-3 xl:grid-cols-[1fr_1.2fr]">
        <Surface className="p-4">
          <h2 className="panel-title">Profile Summary</h2>
          <div className="mt-3 space-y-2 text-sm">
            <p>
              <span className="text-[var(--text-secondary)]">Username:</span> {userName}
            </p>
            <p>
              <span className="text-[var(--text-secondary)]">Roles:</span> {roles.join(", ")}
            </p>
          </div>
        </Surface>

        <Surface className="p-4">
          <h2 className="panel-title">Preferences</h2>
          <form className="mt-3 space-y-3" onSubmit={onSave}>
            <ToggleRow
              label="Email notifications"
              checked={prefs.emailAlerts}
              onChange={(checked) => setPrefs({ ...prefs, emailAlerts: checked })}
            />
            <ToggleRow
              label="In-app alert notifications"
              checked={prefs.inAppAlerts}
              onChange={(checked) => setPrefs({ ...prefs, inAppAlerts: checked })}
            />
            <ToggleRow
              label="Reduced animation"
              checked={prefs.reducedMotion}
              onChange={(checked) => setPrefs({ ...prefs, reducedMotion: checked })}
            />
            <Button type="submit">
              Save Preferences
            </Button>
          </form>
          {savedAt ? <p className="mt-3 text-xs text-[var(--risk-low)]">Saved at {savedAt}</p> : null}
        </Surface>
      </div>
    </PageSection>
  );
}

function ToggleRow({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex w-full items-center justify-between rounded-sm border border-[var(--border-default)] bg-[var(--bg-surface-2)] px-3 py-2 text-left">
      <span className="text-sm">{label}</span>
      <Switch checked={checked} onCheckedChange={onChange} />
    </div>
  );
}
