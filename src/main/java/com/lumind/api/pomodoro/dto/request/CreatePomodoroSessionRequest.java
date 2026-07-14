package com.lumind.api.pomodoro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreatePomodoroSessionRequest(
        @NotNull
        @Min(1)
        @Max(180)
        Integer durationMinutes,

        @NotNull
        Instant startedAt
) {
}
