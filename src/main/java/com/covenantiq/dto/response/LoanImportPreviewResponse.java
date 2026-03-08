package com.covenantiq.dto.response;

import java.util.List;

public record LoanImportPreviewResponse(
        LoanImportBatchResponse batch,
        List<LoanImportRowResponse> rows
) {
}
