package com.lumind.api.statistics.dto.response;

import java.time.Instant;

public record HabitStatisticsResponse(
        Instant from,
        Instant to,
        long totalHabits,
        long createdInPeriod
) {
}
