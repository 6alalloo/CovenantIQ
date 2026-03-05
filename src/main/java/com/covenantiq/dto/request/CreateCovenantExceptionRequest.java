package com.covenantiq.dto.request;

import com.covenantiq.enums.CovenantExceptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateCovenantExceptionRequest(
        @NotNull Long covenantId,
        @NotNull CovenantExceptionType exceptionType,
        @NotBlank String reason,
        @NotNull LocalDate effectiveFrom,
        @NotNull LocalDate effectiveTo,
        String controlsJson
) {
}
