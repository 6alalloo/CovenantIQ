package com.covenantiq.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        List<String> roles,
        OffsetDateTime createdAt
) {
}
