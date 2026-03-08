package com.covenantiq.dto.response;

import com.covenantiq.enums.LoanImportBatchStatus;

import java.time.OffsetDateTime;

public record LoanImportBatchResponse(
        Long id,
        String fileName,
        String uploadedBy,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        LoanImportBatchStatus status,
        int totalRows,
        int validRows,
        int invalidRows,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        String sourceSystem
) {
}
