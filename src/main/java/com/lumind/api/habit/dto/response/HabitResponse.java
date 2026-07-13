package com.lumind.api.habit.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HabitResponse(
        UUID id,
        UUID userId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
