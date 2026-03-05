import { useEffect, useMemo, useState } from "react";
import {
  approveChangeRequest,
  createChangeRequest,
  createRelease,
  getChangeRequests,
  getReleases,
  rollbackRelease,
} from "../api/client";
import { PageSection } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs";
import { formatDateTime, formatEnumLabel } from "../lib/format";
import type { ChangeRequest, ReleaseBatch } from "../types/api";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";

type ChangeControlTab = "queue" | "timeline";
type QueueFilter = "all" | "pending" | "approved" | "released" | "rolledBack";

function requestPriority(request: ChangeRequest) {
  if (request.status === "SUBMITTED") return "Needs approval";
  if (request.status === "APPROVED") return "Ready to release";
  if (request.status === "ROLLED_BACK") return "Rolled back";
  if (request.status === "RELEASED") return "Released";
  return "Draft";
}

function statusBadge(request: ChangeRequest) {
  if (request.status === "APPROVED") return "Validated";
  if (request.status === "ROLLED_BACK") return "Breach";
  if (request.status === "RELEASED") return "Pass";
  if (request.status === "SUBMITTED") return "Open";
  return request.status;
}

export function ChangeControlPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const [changeRequests, setChangeRequests] = useState<ChangeRequest[]>([]);
  const [releases, setReleases] = useState<ReleaseBatch[]>([]);
  const [selectedRequestId, setSelectedRequestId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<ChangeControlTab>("queue");
  const [filter, setFilter] = useState<QueueFilter>("all");
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [busyRequestId, setBusyRequestId] = useState<number | null>(null);
  const [busyReleaseId, setBusyReleaseId] = useState<number | null>(null);

  const load = async () => {
    try {
      setError(null);
      const [requests, nextReleases] = await Promise.all([getChangeRequests(), getReleases()]);
      setChangeRequests(requests);
      setReleases(nextReleases);
      setSelectedRequestId((current) => current ?? requests[0]?.id ?? null);
    } catch (loadError) {
      setError((loadError as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const selectedRequest = useMemo(
    () => changeRequests.find((request) => request.id === selectedRequestId) ?? null,
    [changeRequests, selectedRequestId]
  );

  const filteredRequests = useMemo(() => {
    switch (filter) {
      case "pending":
        return changeRequests.filter((request) => request.status === "SUBMITTED");
      case "approved":
        return changeRequests.filter((request) => request.status === "APPROVED");
      case "released":
        return changeRequests.filter((request) => request.status === "RELEASED");
      case "rolledBack":
        return changeRequests.filter((request) => request.status === "ROLLED_BACK");
      default:
        return changeRequests;
    }
  }, [changeRequests, filter]);

  const metrics = useMemo(
    () => ({
      pending: changeRequests.filter((request) => request.status === "SUBMITTED").length,
      ready: changeRequests.filter((request) => request.status === "APPROVED").length,
      released: changeRequests.filter((request) => request.status === "RELEASED").length,
      rolledBack: releases.filter((release) => release.rollbackOfReleaseId !== null).length,
    }),
    [changeRequests, releases]
  );

  const linkedRelease = useMemo(
    () => releases.find((release) => release.changeRequestId === selectedRequest?.id) ?? null,
    [releases, selectedRequest?.id]
  );

  const createSampleChangeRequest = async () => {
    try {
      setIsCreating(true);
      setError(null);
      await createChangeRequest({
        type: "RULESET",
        justification: "Promote validated covenant exception handling updates",
        items: [
          {
            artifactType: "RULESET_VERSION",
            artifactId: 1,
            fromVersion: "1",
            toVersion: "2",
            diffJson: JSON.stringify({
              summary: "Downgrade approved exception breaches to early warnings before general breach path",
              addedRules: 1,
              removedRules: 0,
            }),
          },
        ],
      });
      await load();
    } catch (createError) {
      setError((createError as Error).message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleApprove = async (id: number) => {
    try {
      setBusyRequestId(id);
      setError(null);
      await approveChangeRequest(id);
      await load();
    } catch (approveError) {
      setError((approveError as Error).message);
    } finally {
      setBusyRequestId(null);
    }
  };

  const handleRelease = async (request: ChangeRequest) => {
    try {
      setBusyRequestId(request.id);
      setError(null);
      await createRelease({ changeRequestId: request.id, releaseTag: `rel-${request.id}` });
      await load();
      setActiveTab("timeline");
    } catch (releaseError) {
      setError((releaseError as Error).message);
    } finally {
      setBusyRequestId(null);
    }
  };

  const handleRollback = async (release: ReleaseBatch) => {
    try {
      setBusyReleaseId(release.id);
      setError(null);
      await rollbackRelease(release.id, {
        targetReleaseId: release.id,
        justification: "Rollback requested after governance review",
      });
      await load();
    } catch (rollbackError) {
      setError((rollbackError as Error).message);
    } finally {
      setBusyReleaseId(null);
    }
  };

  return (
    <PageSection
      title="Change Control"
      subtitle="Review, approve, release, and roll back governed configuration changes with evidence attached."
      action={sampleUxEnabled ? <Button onClick={createSampleChangeRequest} disabled={isCreating}>{isCreating ? "Creating..." : "Create Sample Change Request"}</Button> : undefined}
    >
      {error && <p className="text-sm text-[var(--risk-high)]">{error}</p>}

      <section className="governance-stack">
        <div className="governance-stat-grid governance-stat-grid--four">
          <article className="governance-stat-card">
            <span className="governance-stat-card__label">Pending my approval</span>
            <strong className="font-numeric text-2xl">{metrics.pending}</strong>
          </article>
          <article className="governance-stat-card">
            <span className="governance-stat-card__label">Ready to release</span>
            <strong className="font-numeric text-2xl">{metrics.ready}</strong>
          </article>
          <article className="governance-stat-card">
            <span className="governance-stat-card__label">Released</span>
            <strong className="font-numeric text-2xl">{metrics.released}</strong>
          </article>
          <article className="governance-stat-card">
            <span className="governance-stat-card__label">Rollback events</span>
            <strong className="font-numeric text-2xl">{metrics.rolledBack}</strong>
          </article>
        </div>

        <section className="governance-panel p-5">
          <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as ChangeControlTab)}>
            <TabsList className="mb-5">
              <TabsTrigger value="queue">Approval Queue</TabsTrigger>
              <TabsTrigger value="timeline">Release Timeline</TabsTrigger>
            </TabsList>
          </Tabs>

          {activeTab === "queue" && (
            <div className="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
              <aside className="space-y-4">
                <div className="governance-subpanel">
                  <p className="governance-eyebrow">Queue filters</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <Button variant={filter === "all" ? "default" : "outline"} onClick={() => setFilter("all")}>All</Button>
                    <Button variant={filter === "pending" ? "default" : "outline"} onClick={() => setFilter("pending")}>Needs approval</Button>
                    <Button variant={filter === "approved" ? "default" : "outline"} onClick={() => setFilter("approved")}>Ready</Button>
                    <Button variant={filter === "released" ? "default" : "outline"} onClick={() => setFilter("released")}>Released</Button>
                  </div>
                </div>
                <div className="space-y-3">
                  {filteredRequests.map((request) => (
                    <button
                      key={request.id}
                      className="governance-list-item text-left"
                      data-active={request.id === selectedRequestId}
                      onClick={() => setSelectedRequestId(request.id)}
                      type="button"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Request #{request.id}</p>
                          <p className="mt-1 text-xs text-[var(--text-secondary)]">{formatEnumLabel(request.type)}</p>
                        </div>
                        <Badge>{statusBadge(request)}</Badge>
                      </div>
                      <p className="mt-3 text-sm text-[var(--text-secondary)] line-clamp-2">{request.justification}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-[var(--text-secondary)]">
                        <span>{requestPriority(request)}</span>
                        <span>Requested {formatDateTime(request.requestedAt)}</span>
                        <span>{request.items.length} artifact{request.items.length === 1 ? "" : "s"}</span>
                      </div>
                    </button>
                  ))}
                  {filteredRequests.length === 0 && (
                    <div className="governance-subpanel">
                      <p className="text-sm text-[var(--text-secondary)]">No requests match this filter.</p>
                    </div>
                  )}
                </div>
              </aside>

              <div className="space-y-4">
                {selectedRequest ? (
                  <>
                    <section className="governance-hero card p-5">
                      <div className="governance-hero__meta">
                        <div>
                          <p className="governance-eyebrow">Request Overview</p>
                          <h2 className="text-2xl font-semibold text-[var(--text-primary)]">Change Request #{selectedRequest.id}</h2>
                          <p className="mt-2 max-w-3xl text-sm text-[var(--text-secondary)]">{selectedRequest.justification}</p>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge>{selectedRequest.status}</Badge>
                          <Badge>{requestPriority(selectedRequest)}</Badge>
                        </div>
                      </div>
                      <div className="governance-stat-grid governance-stat-grid--three">
                        <div className="governance-stat-card">
                          <span className="governance-stat-card__label">Requester</span>
                          <strong>{selectedRequest.requestedBy}</strong>
                        </div>
                        <div className="governance-stat-card">
                          <span className="governance-stat-card__label">Approver</span>
                          <strong>{selectedRequest.approvedBy ?? "Pending"}</strong>
                        </div>
                        <div className="governance-stat-card">
                          <span className="governance-stat-card__label">Created</span>
                          <strong>{formatDateTime(selectedRequest.requestedAt)}</strong>
                        </div>
                      </div>
                    </section>

                    <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
                      <div className="space-y-4">
                        <article className="governance-subpanel">
                          <p className="governance-eyebrow">Changes</p>
                          <div className="mt-4 space-y-3">
                            {selectedRequest.items.map((item) => (
                              <div key={item.id} className="governance-timeline-item">
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                  <div>
                                    <p className="text-sm font-semibold text-[var(--text-primary)]">{formatEnumLabel(item.artifactType)}</p>
                                    <p className="mt-1 text-xs text-[var(--text-secondary)]">Artifact #{item.artifactId}</p>
                                  </div>
                                  <Badge>{`${item.fromVersion ?? "-"} -> ${item.toVersion ?? "-"}`}</Badge>
                                </div>
                                <pre className="governance-code-block mt-3">{item.diffJson}</pre>
                              </div>
                            ))}
                          </div>
                        </article>

                        <article className="governance-subpanel">
                          <p className="governance-eyebrow">Validation Evidence</p>
                          <div className="mt-4 grid gap-3 md:grid-cols-2">
                            <div className="governance-diff-card">
                              <span className="governance-diff-card__label">Evidence packet</span>
                              <strong>Attached in change payload</strong>
                            </div>
                            <div className="governance-diff-card">
                              <span className="governance-diff-card__label">Promotion readiness</span>
                              <strong>{selectedRequest.status === "APPROVED" ? "Approved for release" : "Awaiting maker-checker decision"}</strong>
                            </div>
                          </div>
                        </article>

                        <article className="governance-subpanel">
                          <p className="governance-eyebrow">Audit Trail</p>
                          <div className="mt-4 space-y-3">
                            <div className="governance-timeline-item">
                              <p className="text-sm font-semibold text-[var(--text-primary)]">Requested by {selectedRequest.requestedBy}</p>
                              <p className="mt-1 text-xs text-[var(--text-secondary)]">{formatDateTime(selectedRequest.requestedAt)}</p>
                            </div>
                            {selectedRequest.approvedAt && (
                              <div className="governance-timeline-item">
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Approved by {selectedRequest.approvedBy}</p>
                                <p className="mt-1 text-xs text-[var(--text-secondary)]">{formatDateTime(selectedRequest.approvedAt)}</p>
                              </div>
                            )}
                            {linkedRelease && (
                              <div className="governance-timeline-item">
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Released as {linkedRelease.releaseTag}</p>
                                <p className="mt-1 text-xs text-[var(--text-secondary)]">{formatDateTime(linkedRelease.releasedAt)}</p>
                              </div>
                            )}
                          </div>
                        </article>
                      </div>

                      <aside className="space-y-4">
                        <div className="governance-subpanel space-y-3">
                          <p className="governance-eyebrow">Decision</p>
                          <Button
                            onClick={() => handleApprove(selectedRequest.id)}
                            disabled={selectedRequest.status !== "SUBMITTED" || busyRequestId === selectedRequest.id}
                          >
                            {busyRequestId === selectedRequest.id && selectedRequest.status === "SUBMITTED" ? "Approving..." : "Approve Request"}
                          </Button>
                          <Button
                            variant="outline"
                            onClick={() => handleRelease(selectedRequest)}
                            disabled={selectedRequest.status !== "APPROVED" || busyRequestId === selectedRequest.id}
                          >
                            {busyRequestId === selectedRequest.id && selectedRequest.status === "APPROVED" ? "Releasing..." : "Create Release"}
                          </Button>
                          <p className="text-xs text-[var(--text-secondary)]">
                            Approval unlocks release. Release moves the governed change into the production timeline.
                          </p>
                        </div>

                        <div className="governance-subpanel">
                          <p className="governance-eyebrow">Release Readiness</p>
                          <div className="mt-3 space-y-3">
                            <div className="governance-diff-card">
                              <span className="governance-diff-card__label">State</span>
                              <strong>{requestPriority(selectedRequest)}</strong>
                            </div>
                            <div className="governance-diff-card">
                              <span className="governance-diff-card__label">Linked release</span>
                              <strong>{linkedRelease ? linkedRelease.releaseTag : "Not released"}</strong>
                            </div>
                          </div>
                        </div>
                      </aside>
                    </section>
                  </>
                ) : (
                  <div className="governance-subpanel">
                    <p className="text-sm text-[var(--text-secondary)]">Select a request to inspect its diff, evidence, and release controls.</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === "timeline" && (
            <div className="space-y-4">
              {releases.length === 0 && (
                <div className="governance-subpanel">
                  <p className="text-sm text-[var(--text-secondary)]">No releases yet. Approved requests will appear here once promoted.</p>
                </div>
              )}
              {releases.map((release) => (
                <article key={release.id} className="governance-timeline-item">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-sm font-semibold text-[var(--text-primary)]">{release.releaseTag}</h3>
                        <Badge>{release.rollbackOfReleaseId ? "Rolled Back" : "Released"}</Badge>
                      </div>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">
                        Change Request #{release.changeRequestId} released by {release.releasedBy} on {formatDateTime(release.releasedAt)}.
                      </p>
                    </div>
                    <Button variant="outline" onClick={() => handleRollback(release)} disabled={busyReleaseId === release.id}>
                      {busyReleaseId === release.id ? "Rolling back..." : "Rollback to this release"}
                    </Button>
                  </div>
                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    {release.audits.length > 0 ? (
                      release.audits.map((audit) => (
                        <div key={audit.id} className="governance-diff-card">
                          <span className="governance-diff-card__label">{formatEnumLabel(audit.action)}</span>
                          <strong>{audit.actor}</strong>
                          <p className="mt-2 text-xs text-[var(--text-secondary)]">{formatDateTime(audit.timestampUtc)}</p>
                        </div>
                      ))
                    ) : (
                      <div className="governance-diff-card">
                        <span className="governance-diff-card__label">Audit</span>
                        <strong>No audit entries returned</strong>
                      </div>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </PageSection>
  );
}

