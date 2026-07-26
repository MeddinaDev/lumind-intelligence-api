package com.lumind.api.statistics.dto.response;

import java.time.Instant;

public record ProductivityOverviewResponse(
        Instant from,
        Instant to,
        TaskOverviewMetrics tasks,
        PomodoroOverviewMetrics pomodoroSessions,
        HabitOverviewMetrics habits
) {
    public record TaskOverviewMetrics(
            long created,
            long completed,
            double completionRate
    ) {
    }

    public record PomodoroOverviewMetrics(
            long sessionsStarted,
            long sessionsCompleted,
            long totalFocusMinutes,
            double completionRate
    ) {
    }

    public record HabitOverviewMetrics(
            long totalHabits,
            long createdInPeriod
    ) {
    }
}
