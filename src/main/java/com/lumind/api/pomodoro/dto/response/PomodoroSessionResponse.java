package com.lumind.api.pomodoro.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PomodoroSessionResponse(
        UUID id,
        UUID userId,
        Integer durationMinutes,
        Integer completedMinutes,
        Boolean completed,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        
) {
}
