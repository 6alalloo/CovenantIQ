package com.covenantiq.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ReleaseBatchResponse(
        Long id,
        Long changeRequestId,
        String releaseTag,
        String releasedBy,
        OffsetDateTime releasedAt,
        Long rollbackOfReleaseId,
        List<ReleaseAuditResponse> audits
) {
}
