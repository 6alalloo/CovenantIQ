package com.covenantiq.dto.response;

public record PortfolioSummaryResponse(
        long totalActiveLoans,
        long totalBreaches,
        long highRiskLoanCount,
        long mediumRiskLoanCount,
        long lowRiskLoanCount,
        long totalOpenAlerts,
        long totalUnderReviewAlerts
) {
}
