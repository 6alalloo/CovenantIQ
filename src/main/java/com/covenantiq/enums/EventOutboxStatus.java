package com.covenantiq.enums;

public enum EventOutboxStatus {
    PENDING,
    IN_PROGRESS,
    DELIVERED,
    DEAD_LETTER
}
