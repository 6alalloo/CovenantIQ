package com.covenantiq.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWorkflowDefinitionRequest(
        @NotBlank String entityType,
        @NotBlank String name,
        @NotEmpty List<@Valid WorkflowStateInput> states,
        @NotEmpty List<@Valid WorkflowTransitionInput> transitions
) {
    public record WorkflowStateInput(
            @NotBlank String stateCode,
            boolean initial,
            boolean terminal
    ) {
    }

    public record WorkflowTransitionInput(
            @NotBlank String fromState,
            @NotBlank String toState,
            @NotEmpty List<@NotBlank String> allowedRoles,
            List<@NotBlank String> requiredFields,
            String guardExpression
    ) {
    }
}
