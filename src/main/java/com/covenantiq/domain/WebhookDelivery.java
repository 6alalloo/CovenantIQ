package com.covenantiq.domain;

import com.covenantiq.enums.WebhookDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "webhook_delivery",
        indexes = {
                @Index(name = "idx_webhook_delivery_outbox", columnList = "eventOutboxId"),
                @Index(name = "idx_webhook_delivery_subscription", columnList = "subscriptionId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_webhook_delivery_event_subscription", columnNames = {"eventId", "subscriptionId"})
        }
)
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventOutboxId;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private int attemptNo;

    private Integer responseStatus;

    @Column(length = 128)
    private String responseBodyHash;

    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookDeliveryStatus deliveryStatus;

    @Column(length = 64)
    private String errorCode;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(nullable = false)
    private OffsetDateTime attemptedAt;

    @PrePersist
    void prePersist() {
        attemptedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getEventOutboxId() {
        return eventOutboxId;
    }

    public void setEventOutboxId(Long eventOutboxId) {
        this.eventOutboxId = eventOutboxId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBodyHash() {
        return responseBodyHash;
    }

    public void setResponseBodyHash(String responseBodyHash) {
        this.responseBodyHash = responseBodyHash;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public WebhookDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(WebhookDeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
