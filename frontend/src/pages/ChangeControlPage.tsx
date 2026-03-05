import { useEffect, useState } from "react";
import {
  approveChangeRequest,
  createChangeRequest,
  createRelease,
  getChangeRequests,
  getReleases,
  rollbackRelease,
} from "../api/client";
import type { ChangeRequest, ReleaseBatch } from "../types/api";
import { PageSection } from "../components/layout";

export function ChangeControlPage() {
  const [changeRequests, setChangeRequests] = useState<ChangeRequest[]>([]);
  const [releases, setReleases] = useState<ReleaseBatch[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setChangeRequests(await getChangeRequests());
      setReleases(await getReleases());
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <PageSection title="Change Control" subtitle="Maker-checker request queue, releases, and rollback history.">
      {error && <p className="mb-3 text-sm text-[var(--accent-danger)]">{error}</p>}
      <div className="mb-4">
        <button
          className="btn-primary"
          onClick={async () => {
            await createChangeRequest({
              type: "RULESET",
              justification: "Promote validated ruleset",
              items: [{ artifactType: "RULESET_VERSION", artifactId: 1, fromVersion: "1", toVersion: "2", diffJson: "{\"changed\":true}" }],
            });
            await load();
          }}
          type="button"
        >
          Create Change Request
        </button>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card space-y-2">
          <h3 className="text-sm font-semibold">Change Requests</h3>
          {changeRequests.map((cr) => (
            <div key={cr.id} className="rounded border border-[var(--border-default)] p-2">
              <p className="text-sm font-semibold">#{cr.id} {cr.type} | {cr.status}</p>
              <p className="text-xs text-[var(--text-secondary)]">{cr.justification}</p>
              <div className="mt-2 flex gap-2">
                {cr.status === "SUBMITTED" && (
                  <button className="btn-secondary" onClick={() => approveChangeRequest(cr.id).then(load)} type="button">
                    Approve
                  </button>
                )}
                {cr.status === "APPROVED" && (
                  <button className="btn-secondary" onClick={() => createRelease({ changeRequestId: cr.id, releaseTag: `rel-${cr.id}` }).then(load)} type="button">
                    Release
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
        <div className="card space-y-2">
          <h3 className="text-sm font-semibold">Releases</h3>
          {releases.map((release) => (
            <div key={release.id} className="rounded border border-[var(--border-default)] p-2">
              <p className="text-sm font-semibold">{release.releaseTag}</p>
              <p className="text-xs text-[var(--text-secondary)]">{release.releasedAt}</p>
              <button
                className="btn-secondary mt-2"
                onClick={() =>
                  rollbackRelease(release.id, {
                    targetReleaseId: release.id,
                    justification: "Rollback requested by risk lead",
                  }).then(load)
                }
                type="button"
              >
                Rollback
              </button>
            </div>
          ))}
        </div>
      </div>
    </PageSection>
  );
}
