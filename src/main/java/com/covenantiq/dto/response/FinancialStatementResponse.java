package com.covenantiq.dto.response;

import com.covenantiq.enums.PeriodType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FinancialStatementResponse(
        Long id,
        Long loanId,
        PeriodType periodType,
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal currentAssets,
        BigDecimal currentLiabilities,
        BigDecimal totalDebt,
        BigDecimal totalEquity,
        BigDecimal ebit,
        BigDecimal interestExpense,
        OffsetDateTime submissionTimestampUtc
) {
}
