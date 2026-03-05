package com.covenantiq.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PublishWithReasonRequest(@NotBlank String reason) {
}
