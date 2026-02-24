package com.covenantiq.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanRequest(
        @NotBlank String borrowerName,
        @NotNull @DecimalMin("0.01") BigDecimal principalAmount,
        @NotNull LocalDate startDate
) {
}
