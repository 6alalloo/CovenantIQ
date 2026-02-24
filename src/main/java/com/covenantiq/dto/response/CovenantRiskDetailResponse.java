package com.covenantiq.dto.response;

import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.SeverityLevel;

import java.math.BigDecimal;
import java.util.List;

public record CovenantRiskDetailResponse(
        Long covenantId,
        CovenantType covenantType,
        BigDecimal thresholdValue,
        BigDecimal actualValue,
        CovenantResultStatus status,
        SeverityLevel severityLevel,
        List<String> triggeredRuleCodes
) {
}
