package com.covenantiq.dto.request;

import com.covenantiq.enums.PeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SubmitFinancialStatementRequest(
        @NotNull PeriodType periodType,
        @NotNull @Min(2000) @Max(2100) Integer fiscalYear,
        @Min(1) @Max(4) Integer fiscalQuarter,
        @NotNull @DecimalMin("0.00") BigDecimal currentAssets,
        @NotNull @DecimalMin("0.00") BigDecimal currentLiabilities,
        @NotNull @DecimalMin("0.00") BigDecimal totalDebt,
        @NotNull @DecimalMin("0.00") BigDecimal totalEquity,
        @NotNull BigDecimal ebit,
        @NotNull BigDecimal interestExpense,
        OffsetDateTime submissionTimestamp
) {
}
