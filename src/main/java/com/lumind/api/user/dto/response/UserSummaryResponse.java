package com.lumind.api.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt
) {
}
