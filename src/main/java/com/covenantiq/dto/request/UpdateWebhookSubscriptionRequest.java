package com.covenantiq.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateWebhookSubscriptionRequest(
        @Size(min = 1, max = 120) String name,
        @Size(min = 1, max = 512) String endpointUrl,
        String secret,
        List<String> eventFilters,
        Boolean active
) {
}
