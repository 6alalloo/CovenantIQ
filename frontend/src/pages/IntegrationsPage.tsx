import { FormEvent, useEffect, useState } from "react";
import {
  createWebhookSubscription,
  getWebhookDeliveries,
  getWebhookSubscriptions,
  retryWebhookOutboxEvent,
} from "../api/client";
import type { WebhookDelivery, WebhookSubscription } from "../types/api";
import { PageSection } from "../components/layout";

export function IntegrationsPage() {
  const [subscriptions, setSubscriptions] = useState<WebhookSubscription[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [name, setName] = useState("");
  const [endpointUrl, setEndpointUrl] = useState("");
  const [secret, setSecret] = useState("");
  const [eventFilters, setEventFilters] = useState("AlertCreated,AlertStatusChanged");
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setSubscriptions(await getWebhookSubscriptions());
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    if (!selected) return;
    void getWebhookDeliveries(selected).then(setDeliveries).catch((e) => setError((e as Error).message));
  }, [selected]);

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createWebhookSubscription({
        name,
        endpointUrl,
        secret,
        eventFilters: eventFilters.split(",").map((v) => v.trim()).filter(Boolean),
      });
      setName("");
      setEndpointUrl("");
      setSecret("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <PageSection title="Integrations" subtitle="Webhook subscriptions, delivery attempts, and dead-letter retries.">
      {error && <p className="mb-3 text-sm text-[var(--accent-danger)]">{error}</p>}
      <form className="card mb-4 grid gap-2 md:grid-cols-4" onSubmit={onCreate}>
        <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="Name" />
        <input className="input" value={endpointUrl} onChange={(e) => setEndpointUrl(e.target.value)} placeholder="Endpoint URL" />
        <input className="input" value={secret} onChange={(e) => setSecret(e.target.value)} placeholder="Secret" />
        <button className="btn-primary" type="submit">Create Webhook</button>
        <input
          className="input md:col-span-4"
          value={eventFilters}
          onChange={(e) => setEventFilters(e.target.value)}
          placeholder="Comma-separated filters, e.g. AlertCreated,severity:HIGH,loanId:12"
        />
      </form>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card">
          <h3 className="mb-2 text-sm font-semibold">Subscriptions</h3>
          <div className="space-y-2">
            {subscriptions.map((subscription) => (
              <button
                key={subscription.id}
                className={`w-full rounded border p-2 text-left ${selected === subscription.id ? "border-[var(--accent-primary)]" : "border-[var(--border-default)]"}`}
                onClick={() => setSelected(subscription.id)}
                type="button"
              >
                <p className="text-sm font-semibold">{subscription.name}</p>
                <p className="text-xs text-[var(--text-secondary)]">{subscription.endpointUrl}</p>
              </button>
            ))}
          </div>
        </div>
        <div className="card">
          <h3 className="mb-2 text-sm font-semibold">Delivery Log</h3>
          <div className="space-y-2">
            {deliveries.map((delivery) => (
              <div key={delivery.id} className="rounded border border-[var(--border-default)] p-2">
                <p className="text-xs">{delivery.deliveryStatus} | status {delivery.responseStatus ?? "n/a"} | attempt {delivery.attemptNo}</p>
                <p className="text-xs text-[var(--text-secondary)]">{delivery.attemptedAt}</p>
                {delivery.deliveryStatus === "FAILED" && (
                  <button className="btn-secondary mt-2" onClick={() => retryWebhookOutboxEvent(delivery.eventOutboxId)} type="button">
                    Retry Outbox Event
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </PageSection>
  );
}
