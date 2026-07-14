package com.lumind.api.pomodoro.dto.request;

import java.time.Instant;

public record UpdatePomodoroSessionRequest(
        Integer completedMinutes,
        Boolean completed,
        Instant finishedAt
) {
}
