package com.covenantiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReleaseRequest(
        @NotNull Long changeRequestId,
        @NotBlank String releaseTag
) {
}
