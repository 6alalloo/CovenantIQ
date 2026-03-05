package com.covenantiq.dto.response;

import java.util.List;

public record WorkflowTransitionResponse(
        String fromState,
        String toState,
        List<String> allowedRoles,
        List<String> requiredFields,
        String guardExpression
) {
}
