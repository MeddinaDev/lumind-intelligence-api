package com.lumind.api.ai.dto.response;

import java.time.Instant;
import java.util.List;

public record ProductivityAnalysisResponse(
        Instant from,
        Instant to,
        Instant generatedAt,
        String summary,
        List<String> insights,
        List<String> recommendations
) {
}
