package com.lumind.api.statistics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Optional UTC time range for productivity statistics queries")
public record StatisticsPeriodQuery(
        @Schema(
                description = "Period start (inclusive, ISO-8601 UTC). Defaults to start of day UTC 30 days before now.",
                example = "2026-06-26T00:00:00Z"
        )
        Instant from,

        @Schema(
                description = "Period end (inclusive, ISO-8601 UTC). Defaults to now.",
                example = "2026-07-26T23:59:59Z"
        )
        Instant to
) {
}
