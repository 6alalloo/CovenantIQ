package com.covenantiq.dto.response;

import com.covenantiq.enums.RulesetDomain;

import java.time.OffsetDateTime;

public record RulesetResponse(
        Long id,
        String key,
        String name,
        RulesetDomain domain,
        String ownerRole,
        String createdBy,
        OffsetDateTime createdAt
) {
}
