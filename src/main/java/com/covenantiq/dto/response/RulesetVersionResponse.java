package com.covenantiq.dto.response;

import com.covenantiq.enums.RulesetVersionStatus;

import java.time.OffsetDateTime;

public record RulesetVersionResponse(
        Long id,
        Long rulesetId,
        int version,
        RulesetVersionStatus status,
        String definitionJson,
        int schemaVersion,
        String changeSummary,
        String createdBy,
        String approvedBy,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {
}
