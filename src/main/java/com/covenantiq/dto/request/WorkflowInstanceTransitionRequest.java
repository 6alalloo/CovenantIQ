package com.covenantiq.dto.request;

import java.util.Map;

public record WorkflowInstanceTransitionRequest(
        String toState,
        String reason,
        Map<String, Object> metadata
) {
}
