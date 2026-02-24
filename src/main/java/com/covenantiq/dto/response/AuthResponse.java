package com.covenantiq.dto.response;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username,
        List<String> roles
) {
}
