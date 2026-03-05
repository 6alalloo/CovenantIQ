package com.covenantiq.dto.request;

import com.covenantiq.enums.RulesetDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRulesetRequest(
        @NotBlank String key,
        @NotBlank String name,
        @NotNull RulesetDomain domain,
        @NotBlank String ownerRole
) {
}
