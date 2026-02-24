package com.covenantiq.dto.response;

import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.SeverityLevel;

import java.math.BigDecimal;

public record CovenantResponse(
        Long id,
        Long loanId,
        CovenantType type,
        BigDecimal thresholdValue,
        ComparisonType comparisonType,
        SeverityLevel severityLevel
) {
}
