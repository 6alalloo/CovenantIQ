package com.covenantiq.dto.response;

import java.time.OffsetDateTime;

public record AttachmentMetadataResponse(
        Long id,
        String filename,
        Long fileSize,
        String contentType,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {
}
