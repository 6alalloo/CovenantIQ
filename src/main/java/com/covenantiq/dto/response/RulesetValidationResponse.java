package com.covenantiq.dto.response;

import java.util.Map;

public record RulesetValidationResponse(
        Long rulesetVersionId,
        boolean valid,
        boolean pass,
        Map<String, Object> actualOutput,
        String message
) {
}
