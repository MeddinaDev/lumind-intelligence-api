package com.lumind.api.auth.dto.response;

import com.lumind.api.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(
                description = "JWT access token for authenticated API requests",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(
                description = "JWT refresh token for obtaining a new token pair",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String refreshToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Authenticated user public profile")
        UserSummaryResponse user
) {
}
