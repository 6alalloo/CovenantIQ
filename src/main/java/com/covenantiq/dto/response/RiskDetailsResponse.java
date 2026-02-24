package com.covenantiq.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record RiskDetailsResponse(
        Long loanId,
        Long financialStatementId,
        OffsetDateTime evaluationTimestampUtc,
        List<CovenantRiskDetailResponse> details
) {
}
