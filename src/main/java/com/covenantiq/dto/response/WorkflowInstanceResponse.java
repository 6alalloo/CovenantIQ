package com.covenantiq.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkflowInstanceResponse(
        Long id,
        String entityType,
        Long entityId,
        Long workflowDefinitionId,
        String currentState,
        OffsetDateTime startedAt,
        OffsetDateTime updatedAt,
        List<WorkflowTransitionLogResponse> transitionLog
) {
}
