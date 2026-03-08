package com.covenantiq.dto.response;

import com.covenantiq.enums.LoanImportRowAction;

public record LoanImportRowResponse(
        Long id,
        int rowNumber,
        String sourceSystem,
        String externalLoanId,
        String borrowerName,
        LoanImportRowAction action,
        String validationMessage,
        Long loanId
) {
}
