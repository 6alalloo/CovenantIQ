package com.covenantiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RollbackReleaseRequest(
        @NotNull Long targetReleaseId,
        @NotBlank String justification
) {
}
