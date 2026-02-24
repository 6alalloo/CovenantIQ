package com.covenantiq.dto.request;

import com.covenantiq.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAlertStatusRequest(
        @NotNull AlertStatus status,
        @Size(max = 2000) String resolutionNotes
) {
}
