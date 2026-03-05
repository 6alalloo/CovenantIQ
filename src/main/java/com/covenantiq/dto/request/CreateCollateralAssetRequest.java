package com.covenantiq.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCollateralAssetRequest(
        @NotBlank String assetType,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal nominalValue,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal haircutPct,
        @NotNull Integer lienRank,
        @NotBlank String currency,
        @NotNull LocalDate effectiveDate
) {
}
