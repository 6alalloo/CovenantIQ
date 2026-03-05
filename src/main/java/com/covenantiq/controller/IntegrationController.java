package com.covenantiq.controller;

import com.covenantiq.domain.WebhookDelivery;
import com.covenantiq.domain.WebhookSubscription;
import com.covenantiq.dto.request.CreateWebhookSubscriptionRequest;
import com.covenantiq.dto.request.UpdateWebhookSubscriptionRequest;
import com.covenantiq.dto.response.WebhookDeliveryResponse;
import com.covenantiq.dto.response.WebhookSubscriptionResponse;
import com.covenantiq.service.WebhookIntegrationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/webhooks")
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationController {

    private final WebhookIntegrationService webhookIntegrationService;

    public IntegrationController(WebhookIntegrationService webhookIntegrationService) {
        this.webhookIntegrationService = webhookIntegrationService;
    }

    @PostMapping
    public WebhookSubscriptionResponse create(@Valid @RequestBody CreateWebhookSubscriptionRequest request) {
        return toResponse(webhookIntegrationService.create(request));
    }

    @GetMapping
    public List<WebhookSubscriptionResponse> list() {
        return webhookIntegrationService.list().stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}")
    public WebhookSubscriptionResponse patch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWebhookSubscriptionRequest request
    ) {
        return toResponse(webhookIntegrationService.patch(id, request));
    }

    @GetMapping("/{id}/deliveries")
    public List<WebhookDeliveryResponse> deliveries(@PathVariable Long id) {
        return webhookIntegrationService.listDeliveries(id).stream().map(this::toResponse).toList();
    }

    @PostMapping("/deliveries/{eventOutboxId}/retry")
    public void retryDeadLetter(@PathVariable Long eventOutboxId) {
        webhookIntegrationService.markOutboxRetry(eventOutboxId);
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription subscription) {
        List<String> filters = Arrays.stream(subscription.getEventFiltersCsv().split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
        return new WebhookSubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getEndpointUrl(),
                filters,
                subscription.isActive(),
                subscription.getCreatedBy(),
                subscription.getCreatedAt()
        );
    }

    private WebhookDeliveryResponse toResponse(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getEventOutboxId(),
                delivery.getEventId(),
                delivery.getSubscriptionId(),
                delivery.getAttemptNo(),
                delivery.getResponseStatus(),
                delivery.getResponseBodyHash(),
                delivery.getLatencyMs(),
                delivery.getDeliveryStatus(),
                delivery.getErrorCode(),
                delivery.getAttemptedAt()
        );
    }
}
