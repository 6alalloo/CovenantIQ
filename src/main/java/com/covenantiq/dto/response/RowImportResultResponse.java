package com.covenantiq.dto.response;

public record RowImportResultResponse(
        int rowNumber,
        boolean success,
        String errorMessage
) {
}
