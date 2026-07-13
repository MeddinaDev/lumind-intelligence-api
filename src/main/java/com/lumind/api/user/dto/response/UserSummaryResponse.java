package com.lumind.api.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "User email address", example = "maria.garcia@example.com")
        String email,

        @Schema(description = "First name", example = "María")
        String firstName,

        @Schema(description = "Last name", example = "García")
        String lastName,

        @Schema(description = "Account creation timestamp (ISO-8601 UTC)", example = "2026-07-06T15:30:00Z")
        Instant createdAt
) {
}
