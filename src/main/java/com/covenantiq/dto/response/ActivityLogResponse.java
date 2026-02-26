package com.covenantiq.dto.response;

import com.covenantiq.enums.ActivityEventType;

import java.time.OffsetDateTime;

public record ActivityLogResponse(
        Long id,
        ActivityEventType eventType,
        String entityType,
        Long entityId,
        String username,
        OffsetDateTime timestampUtc,
        String description,
        Long loanId
) {
}
