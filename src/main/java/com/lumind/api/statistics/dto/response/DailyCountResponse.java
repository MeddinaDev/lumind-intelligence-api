package com.lumind.api.statistics.dto.response;

import java.time.LocalDate;

public record DailyCountResponse(
        LocalDate date,
        long count
) {
}
