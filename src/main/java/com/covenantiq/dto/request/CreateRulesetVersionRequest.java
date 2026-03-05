package com.covenantiq.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateRulesetVersionRequest(
        @NotBlank String definitionJson,
        @Min(1) int schemaVersion,
        String changeSummary
) {
}
