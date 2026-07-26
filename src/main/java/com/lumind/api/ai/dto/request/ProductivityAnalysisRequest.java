package com.lumind.api.ai.dto.request;

import java.time.Instant;

public record ProductivityAnalysisRequest(
        Instant from,
        Instant to
) {
}
