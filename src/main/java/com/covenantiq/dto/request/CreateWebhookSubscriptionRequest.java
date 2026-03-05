package com.covenantiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWebhookSubscriptionRequest(
        @NotBlank String name,
        @NotBlank String endpointUrl,
        @NotBlank String secret,
        @NotEmpty List<@NotBlank String> eventFilters
) {
}
