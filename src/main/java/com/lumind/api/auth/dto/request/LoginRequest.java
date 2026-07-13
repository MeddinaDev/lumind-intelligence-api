package com.lumind.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Registered email address", example = "maria.garcia@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "Account password", example = "SecurePass123")
        @NotBlank
        String password
) {
}
