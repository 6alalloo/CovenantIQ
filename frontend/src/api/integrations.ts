import type { WebhookDelivery, WebhookSubscription } from "../types/api";
import { request } from "./core/http";

export function createWebhookSubscription(payload: {
  name: string;
  endpointUrl: string;
  secret: string;
  eventFilters: string[];
}) {
  return request<WebhookSubscription>("/integrations/webhooks", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getWebhookSubscriptions() {
  return request<WebhookSubscription[]>("/integrations/webhooks");
}

export function updateWebhookSubscription(
  id: number,
  payload: Partial<{ name: string; endpointUrl: string; secret: string; eventFilters: string[]; active: boolean }>
) {
  return request<WebhookSubscription>(`/integrations/webhooks/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function getWebhookDeliveries(id: number) {
  return request<WebhookDelivery[]>(`/integrations/webhooks/${id}/deliveries`);
}

export function retryWebhookOutboxEvent(eventOutboxId: number) {
  return request<void>(`/integrations/webhooks/deliveries/${eventOutboxId}/retry`, { method: "POST" });
}
