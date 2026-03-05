package com.covenantiq.dto.response;

public record WorkflowStateResponse(
        String stateCode,
        boolean initial,
        boolean terminal
) {
}
