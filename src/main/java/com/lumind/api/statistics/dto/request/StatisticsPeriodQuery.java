package com.lumind.api.statistics.dto.request;

import java.time.Instant;

public record StatisticsPeriodQuery(
        Instant from,
        Instant to
) {
}
