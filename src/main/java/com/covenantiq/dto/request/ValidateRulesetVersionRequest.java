package com.covenantiq.dto.request;

import java.util.Map;

public record ValidateRulesetVersionRequest(
        Map<String, Object> input,
        Map<String, Object> expectedOutput
) {
}
