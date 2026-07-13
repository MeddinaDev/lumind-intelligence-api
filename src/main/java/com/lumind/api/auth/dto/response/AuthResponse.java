package com.lumind.api.auth.dto.response;

import com.lumind.api.user.dto.response.UserSummaryResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
}
