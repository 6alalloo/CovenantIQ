package com.covenantiq.dto.response;

public record ChangeRequestItemResponse(
        Long id,
        String artifactType,
        Long artifactId,
        String fromVersion,
        String toVersion,
        String diffJson
) {
}
