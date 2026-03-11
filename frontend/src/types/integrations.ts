export type WebhookSubscription = {
  id: number;
  name: string;
  endpointUrl: string;
  eventFilters: string[];
  active: boolean;
  createdBy: string;
  createdAt: string;
};

export type WebhookDelivery = {
  id: number;
  eventOutboxId: number;
  eventId: string;
  subscriptionId: number;
  attemptNo: number;
  responseStatus: number | null;
  responseBodyHash: string | null;
  latencyMs: number | null;
  deliveryStatus: "SUCCESS" | "FAILED";
  errorCode: string | null;
  attemptedAt: string;
};
