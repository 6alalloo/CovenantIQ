package com.covenantiq.dto.response;

import java.time.OffsetDateTime;

public record WorkflowTransitionLogResponse(
        Long id,
        String fromState,
        String toState,
        String actor,
        String reason,
        String metadataJson,
        OffsetDateTime timestampUtc
) {
}
