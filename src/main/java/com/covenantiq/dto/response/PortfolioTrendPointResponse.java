package com.covenantiq.dto.response;

public record PortfolioTrendPointResponse(
        String periodLabel,
        long highRiskLoanCount,
        long mediumRiskLoanCount,
        long lowRiskLoanCount
) {
}
