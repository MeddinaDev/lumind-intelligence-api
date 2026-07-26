package com.lumind.api.statistics.dto.response;

import java.time.Instant;
import java.util.List;

public record PomodoroStatisticsResponse(
        Instant from,
        Instant to,
        long sessionsStarted,
        long sessionsCompleted,
        long totalFocusMinutes,
        long averageFocusMinutesPerCompletedSession,
        double completionRate,
        List<DailyMinutesResponse> focusMinutesByDay
) {
}
