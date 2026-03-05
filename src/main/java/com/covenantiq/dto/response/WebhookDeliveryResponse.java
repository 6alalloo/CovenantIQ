package com.covenantiq.dto.response;

import com.covenantiq.enums.WebhookDeliveryStatus;

import java.time.OffsetDateTime;

public record WebhookDeliveryResponse(
        Long id,
        Long eventOutboxId,
        String eventId,
        Long subscriptionId,
        int attemptNo,
        Integer responseStatus,
        String responseBodyHash,
        Long latencyMs,
        WebhookDeliveryStatus deliveryStatus,
        String errorCode,
        OffsetDateTime attemptedAt
) {
}
