package com.covenantiq.dto.response;

import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.SeverityLevel;

import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        Long loanId,
        Long financialStatementId,
        AlertType alertType,
        String message,
        SeverityLevel severityLevel,
        String alertRuleCode,
        OffsetDateTime triggeredTimestampUtc,
        AlertStatus status,
        String acknowledgedBy,
        OffsetDateTime acknowledgedAt,
        String resolvedBy,
        OffsetDateTime resolvedAt,
        String resolutionNotes
) {
}
