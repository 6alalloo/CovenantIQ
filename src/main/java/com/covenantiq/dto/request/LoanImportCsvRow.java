package com.covenantiq.dto.request;

import com.covenantiq.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LoanImportCsvRow(
        int rowNumber,
        String sourceSystem,
        String externalLoanId,
        String borrowerName,
        BigDecimal principalAmount,
        LocalDate startDate,
        LoanStatus status,
        OffsetDateTime sourceUpdatedAt
) {
}
