package com.covenantiq.dto.response;

import java.util.List;

public record BulkImportSummaryResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<RowImportResultResponse> rowResults
) {
}
