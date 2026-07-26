package com.lumind.api.statistics.repository.projection;

import java.time.LocalDate;

public record DailyCountAggregation(
        LocalDate date,
        long count
) {
}
