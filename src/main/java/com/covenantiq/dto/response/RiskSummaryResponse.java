package com.covenantiq.dto.response;

import com.covenantiq.enums.RiskLevel;

public record RiskSummaryResponse(
        long totalCovenants,
        long breachedCount,
        long activeWarnings,
        RiskLevel overallRiskLevel
) {
}
