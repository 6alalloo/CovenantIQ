package com.covenantiq.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record WebhookSubscriptionResponse(
        Long id,
        String name,
        String endpointUrl,
        List<String> eventFilters,
        boolean active,
        String createdBy,
        OffsetDateTime createdAt
) {
}
