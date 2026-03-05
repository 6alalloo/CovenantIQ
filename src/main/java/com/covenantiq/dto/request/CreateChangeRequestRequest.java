package com.covenantiq.dto.request;

import com.covenantiq.enums.ChangeRequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateChangeRequestRequest(
        @NotNull ChangeRequestType type,
        @NotBlank String justification,
        @NotEmpty List<@Valid ChangeRequestItemInput> items
) {
    public record ChangeRequestItemInput(
            @NotBlank String artifactType,
            @NotNull Long artifactId,
            String fromVersion,
            String toVersion,
            @NotBlank String diffJson
    ) {
    }
}
