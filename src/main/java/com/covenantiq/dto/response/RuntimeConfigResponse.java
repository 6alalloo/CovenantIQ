package com.covenantiq.dto.response;

public record RuntimeConfigResponse(
        String backendMode,
        boolean demoMode,
        boolean testMode,
        boolean sampleContentAvailable,
        boolean strictSecretValidationEnabled
) {
}
