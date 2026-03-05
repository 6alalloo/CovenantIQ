package com.covenantiq.repository;

import com.covenantiq.domain.WebhookDelivery;
import com.covenantiq.enums.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByEventOutboxIdOrderByAttemptedAtDesc(Long eventOutboxId);

    List<WebhookDelivery> findBySubscriptionIdOrderByAttemptedAtDesc(Long subscriptionId);

    Optional<WebhookDelivery> findByEventIdAndSubscriptionIdAndDeliveryStatus(
            String eventId,
            Long subscriptionId,
            WebhookDeliveryStatus status
    );
}
