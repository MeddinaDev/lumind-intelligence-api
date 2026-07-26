package com.lumind.api.statistics.dto.response;

import java.time.LocalDate;

public record DailyMinutesResponse(
        LocalDate date,
        long minutes
) {
}
