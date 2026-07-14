package com.lumind.api.task.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        boolean completed,
        Instant createdAt,
        Instant updatedAt
) {
}
