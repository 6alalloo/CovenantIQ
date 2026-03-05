package com.covenantiq.service;

import com.covenantiq.domain.EventOutbox;
import com.covenantiq.domain.WebhookDelivery;
import com.covenantiq.domain.WebhookSubscription;
import com.covenantiq.enums.EventOutboxStatus;
import com.covenantiq.enums.WebhookDeliveryStatus;
import com.covenantiq.repository.EventOutboxRepository;
import com.covenantiq.repository.WebhookDeliveryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OutboxDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcherService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Set<Integer> RETRYABLE_HTTP = Set.of(408, 429, 500, 502, 503, 504);

    private final EventOutboxRepository eventOutboxRepository;
    private final WebhookIntegrationService webhookIntegrationService;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxDispatcherService(
            EventOutboxRepository eventOutboxRepository,
            WebhookIntegrationService webhookIntegrationService,
            WebhookDeliveryRepository webhookDeliveryRepository,
            ObjectMapper objectMapper,
            @Value("${app.events.dispatch.batch-size:25}") int batchSize,
            @Value("${app.events.dispatch.max-attempts:6}") int maxAttempts
    ) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.webhookIntegrationService = webhookIntegrationService;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Scheduled(fixedDelayString = "${app.events.dispatch.fixed-delay-ms:3000}")
    @Transactional
    public void dispatchPending() {
        List<EventOutbox> events = eventOutboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                List.of(EventOutboxStatus.PENDING, EventOutboxStatus.IN_PROGRESS),
                OffsetDateTime.now(ZoneOffset.UTC),
                PageRequest.of(0, batchSize)
        );
        for (EventOutbox event : events) {
            processEvent(event);
        }
    }

    private void processEvent(EventOutbox event) {
        event.setStatus(EventOutboxStatus.IN_PROGRESS);
        eventOutboxRepository.save(event);
        boolean anyFailure = false;
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayloadJson(), MAP_TYPE);
            for (WebhookSubscription subscription : webhookIntegrationService.activeSubscriptions()) {
                if (!matches(subscription.getEventFiltersCsv(), event.getEventType(), payload)) {
                    continue;
                }
                if (alreadySucceeded(event.getEventId(), subscription.getId())) {
                    continue;
                }
                DeliveryOutcome outcome = deliver(event, payload, subscription);
                if (!outcome.success()) {
                    anyFailure = true;
                }
            }

            if (anyFailure) {
                markForRetry(event);
            } else {
                event.setStatus(EventOutboxStatus.DELIVERED);
                eventOutboxRepository.save(event);
            }
        } catch (Exception ex) {
            log.error("Outbox dispatch failed: eventOutboxId={}, eventId={}", event.getId(), event.getEventId(), ex);
            markForRetry(event);
        }
    }

    private boolean alreadySucceeded(String eventId, Long subscriptionId) {
        return webhookDeliveryRepository.findByEventIdAndSubscriptionIdAndDeliveryStatus(
                eventId,
                subscriptionId,
                WebhookDeliveryStatus.SUCCESS
        ).isPresent();
    }

    private DeliveryOutcome deliver(EventOutbox event, Map<String, Object> payload, WebhookSubscription subscription) {
        long startNs = System.nanoTime();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String secret = webhookIntegrationService.rawSecret(subscription);
            String signature = hmacSha256(secret, payloadJson);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getEndpointUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-CovenantIQ-Event-Id", event.getEventId())
                    .header("X-CovenantIQ-Event-Type", event.getEventType())
                    .header("X-CovenantIQ-Event-Version", "1")
                    .header("X-CovenantIQ-Signature", "sha256=" + signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            saveDelivery(event, subscription, payloadJson, response.statusCode(), response.body(), latencyMs, success, null);
            if (!success && !RETRYABLE_HTTP.contains(response.statusCode())) {
                return new DeliveryOutcome(true);
            }
            return new DeliveryOutcome(success);
        } catch (Exception ex) {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            saveDelivery(event, subscription, event.getPayloadJson(), null, ex.getMessage(), latencyMs, false, "NETWORK_ERROR");
            return new DeliveryOutcome(false);
        }
    }

    private void saveDelivery(
            EventOutbox event,
            WebhookSubscription subscription,
            String payloadJson,
            Integer responseStatus,
            String responseBody,
            long latencyMs,
            boolean success,
            String errorCode
    ) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setEventOutboxId(event.getId());
        delivery.setEventId(event.getEventId());
        delivery.setSubscriptionId(subscription.getId());
        delivery.setAttemptNo(event.getAttemptCount() + 1);
        delivery.setPayloadJson(payloadJson);
        delivery.setResponseStatus(responseStatus);
        delivery.setResponseBodyHash(responseBody == null ? null : sha256Hex(responseBody));
        delivery.setLatencyMs(latencyMs);
        delivery.setDeliveryStatus(success ? WebhookDeliveryStatus.SUCCESS : WebhookDeliveryStatus.FAILED);
        delivery.setErrorCode(errorCode);
        webhookDeliveryRepository.save(delivery);
    }

    private void markForRetry(EventOutbox event) {
        int nextAttempt = event.getAttemptCount() + 1;
        event.setAttemptCount(nextAttempt);
        if (nextAttempt >= maxAttempts) {
            event.setStatus(EventOutboxStatus.DEAD_LETTER);
            event.setNextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            long delaySeconds = (long) Math.pow(2, Math.min(nextAttempt, 10));
            event.setStatus(EventOutboxStatus.PENDING);
            event.setNextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
        }
        eventOutboxRepository.save(event);
    }

    private boolean matches(String eventFiltersCsv, String eventType, Map<String, Object> payload) {
        if (eventFiltersCsv == null || eventFiltersCsv.isBlank()) {
            return true;
        }
        String[] filters = eventFiltersCsv.split(",");
        for (String raw : filters) {
            String filter = raw.trim();
            if (filter.isBlank()) {
                continue;
            }
            if (filter.equalsIgnoreCase(eventType)) {
                return true;
            }
            if (filter.startsWith("severity:")) {
                Object severity = payload.get("severity");
                if (severity != null && filter.substring("severity:".length()).equalsIgnoreCase(String.valueOf(severity))) {
                    return true;
                }
            }
            if (filter.startsWith("loanId:")) {
                Object loanId = payload.get("loanId");
                if (loanId != null && filter.substring("loanId:".length()).equals(String.valueOf(loanId))) {
                    return true;
                }
            }
            if ("portfolio:all".equalsIgnoreCase(filter)) {
                return true;
            }
        }
        return false;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmacSha256(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign webhook payload");
        }
    }

    private record DeliveryOutcome(boolean success) {
    }
}
