package com.covenantiq.dto.response;

import java.util.List;

public record LoanImportExecuteResponse(
        LoanImportBatchResponse batch,
        List<LoanImportRowResponse> rows
) {
}
