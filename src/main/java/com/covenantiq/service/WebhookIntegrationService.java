package com.covenantiq.service;

import com.covenantiq.domain.EventOutbox;
import com.covenantiq.domain.WebhookDelivery;
import com.covenantiq.domain.WebhookSubscription;
import com.covenantiq.dto.request.CreateWebhookSubscriptionRequest;
import com.covenantiq.dto.request.UpdateWebhookSubscriptionRequest;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.repository.EventOutboxRepository;
import com.covenantiq.repository.WebhookDeliveryRepository;
import com.covenantiq.repository.WebhookSubscriptionRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WebhookIntegrationService {

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final EventOutboxRepository eventOutboxRepository;
    private final WebhookSecretCodec webhookSecretCodec;
    private final CurrentUserService currentUserService;

    public WebhookIntegrationService(
            WebhookSubscriptionRepository webhookSubscriptionRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            EventOutboxRepository eventOutboxRepository,
            WebhookSecretCodec webhookSecretCodec,
            CurrentUserService currentUserService
    ) {
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.eventOutboxRepository = eventOutboxRepository;
        this.webhookSecretCodec = webhookSecretCodec;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public WebhookSubscription create(CreateWebhookSubscriptionRequest request) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setName(request.name().trim());
        subscription.setEndpointUrl(request.endpointUrl().trim());
        subscription.setSecretEncrypted(webhookSecretCodec.encode(request.secret().trim()));
        subscription.setEventFiltersCsv(toCsv(request.eventFilters()));
        subscription.setActive(true);
        subscription.setCreatedBy(currentUserService.usernameOrSystem());
        return webhookSubscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> list() {
        return webhookSubscriptionRepository.findAll();
    }

    @Transactional
    public WebhookSubscription patch(Long id, UpdateWebhookSubscriptionRequest request) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription " + id + " not found"));

        Optional.ofNullable(request.name()).ifPresent(v -> subscription.setName(v.trim()));
        Optional.ofNullable(request.endpointUrl()).ifPresent(v -> subscription.setEndpointUrl(v.trim()));
        Optional.ofNullable(request.secret()).ifPresent(v -> subscription.setSecretEncrypted(webhookSecretCodec.encode(v.trim())));
        Optional.ofNullable(request.eventFilters()).ifPresent(v -> subscription.setEventFiltersCsv(toCsv(v)));
        Optional.ofNullable(request.active()).ifPresent(subscription::setActive);

        return webhookSubscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<WebhookDelivery> listDeliveries(Long subscriptionId) {
        webhookSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription " + subscriptionId + " not found"));
        return webhookDeliveryRepository.findBySubscriptionIdOrderByAttemptedAtDesc(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> activeSubscriptions() {
        return webhookSubscriptionRepository.findByActiveTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public String rawSecret(WebhookSubscription subscription) {
        return webhookSecretCodec.decode(subscription.getSecretEncrypted());
    }

    @Transactional
    public void markOutboxRetry(Long outboxId) {
        EventOutbox eventOutbox = eventOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event " + outboxId + " not found"));
        eventOutbox.setStatus(com.covenantiq.enums.EventOutboxStatus.PENDING);
        eventOutbox.setAttemptCount(0);
        eventOutbox.setNextAttemptAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        eventOutboxRepository.save(eventOutbox);
    }

    private String toCsv(List<String> filters) {
        return filters.stream()
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.joining(","));
    }
}
