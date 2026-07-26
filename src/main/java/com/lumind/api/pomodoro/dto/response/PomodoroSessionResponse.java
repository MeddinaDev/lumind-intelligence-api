package com.lumind.api.pomodoro.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PomodoroSessionResponse(
        UUID id,
        UUID userId,
        Integer durationMinutes,
        Integer completedMinutes,
        Boolean completed,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
