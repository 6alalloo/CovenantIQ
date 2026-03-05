package com.covenantiq.dto.response;

import com.covenantiq.enums.WorkflowDefinitionStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkflowDefinitionResponse(
        Long id,
        String entityType,
        String name,
        int version,
        WorkflowDefinitionStatus status,
        String createdBy,
        OffsetDateTime createdAt,
        List<WorkflowStateResponse> states,
        List<WorkflowTransitionResponse> transitions
) {
}
