package com.covenantiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CollateralAssetResponse(
        Long id,
        Long loanId,
        String assetType,
        String description,
        BigDecimal nominalValue,
        BigDecimal haircutPct,
        BigDecimal netEligibleValue,
        Integer lienRank,
        String currency,
        LocalDate effectiveDate,
        OffsetDateTime createdAt
) {
}
