package com.lumind.api.pomodoro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record UpdatePomodoroSessionRequest(
        @Min(0)
        @Max(180)
        Integer completedMinutes,

        Boolean completed,

        Instant finishedAt
) {
}
