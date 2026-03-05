package com.covenantiq.dto.response;

import java.time.OffsetDateTime;

public record ReleaseAuditResponse(
        Long id,
        String action,
        String actor,
        String detailsJson,
        OffsetDateTime timestampUtc
) {
}
