package com.lumind.api.statistics.repository.projection;

import java.time.LocalDate;

public record DailyMinutesAggregation(
        LocalDate date,
        long minutes
) {
}
