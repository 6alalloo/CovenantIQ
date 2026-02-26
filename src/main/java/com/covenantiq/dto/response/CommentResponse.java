package com.covenantiq.dto.response;

import java.time.OffsetDateTime;

public record CommentResponse(
        Long id,
        Long loanId,
        String commentText,
        String createdBy,
        OffsetDateTime createdAt
) {
}
