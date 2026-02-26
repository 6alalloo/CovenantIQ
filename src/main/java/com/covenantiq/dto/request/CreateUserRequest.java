package com.covenantiq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Email String email,
        @NotEmpty List<String> roles
) {
}
