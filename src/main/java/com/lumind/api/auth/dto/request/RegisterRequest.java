package com.lumind.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Unique email address of the user", example = "maria.garcia@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Password (8-128 characters)", example = "SecurePass123")
        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @Schema(description = "First name", example = "María")
        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,

        @Schema(description = "Last name", example = "García")
        @NotBlank
        @Size(min = 1, max = 100)
        String lastName
) {
}
