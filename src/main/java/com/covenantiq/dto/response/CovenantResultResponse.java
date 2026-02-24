package com.covenantiq.dto.response;

import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.CovenantType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CovenantResultResponse(
        Long id,
        Long covenantId,
        CovenantType covenantType,
        Long financialStatementId,
        BigDecimal actualValue,
        CovenantResultStatus status,
        OffsetDateTime evaluationTimestampUtc
) {
}
