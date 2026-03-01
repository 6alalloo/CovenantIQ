package com.covenantiq.dto.request;

import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.SeverityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCovenantRequest(
        @NotNull @DecimalMin("0.000001") BigDecimal thresholdValue,
        @NotNull ComparisonType comparisonType,
        @NotNull SeverityLevel severityLevel
) {
}
