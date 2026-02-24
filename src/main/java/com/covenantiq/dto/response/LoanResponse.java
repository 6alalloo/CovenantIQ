package com.covenantiq.dto.response;

import com.covenantiq.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        String borrowerName,
        BigDecimal principalAmount,
        LocalDate startDate,
        LoanStatus status
) {
}
