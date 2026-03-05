package com.covenantiq.dto.response;

import com.covenantiq.enums.CovenantExceptionStatus;
import com.covenantiq.enums.CovenantExceptionType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CovenantExceptionResponse(
        Long id,
        Long loanId,
        Long covenantId,
        CovenantExceptionType exceptionType,
        String reason,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        CovenantExceptionStatus status,
        String requestedBy,
        String approvedBy,
        OffsetDateTime approvedAt,
        String controlsJson,
        OffsetDateTime createdAt
) {
}
