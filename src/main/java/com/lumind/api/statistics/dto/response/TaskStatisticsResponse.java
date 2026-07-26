package com.lumind.api.statistics.dto.response;

import java.time.Instant;
import java.util.List;

public record TaskStatisticsResponse(
        Instant from,
        Instant to,
        long created,
        long completed,
        long pendingCreatedInPeriod,
        double completionRate,
        List<DailyCountResponse> completedByDay
) {
}
