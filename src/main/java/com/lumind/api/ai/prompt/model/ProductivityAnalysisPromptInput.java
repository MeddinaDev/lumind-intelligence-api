package com.lumind.api.ai.prompt.model;

import com.lumind.api.statistics.dto.response.HabitStatisticsResponse;
import com.lumind.api.statistics.dto.response.PomodoroStatisticsResponse;
import com.lumind.api.statistics.dto.response.ProductivityOverviewResponse;
import com.lumind.api.statistics.dto.response.TaskStatisticsResponse;

import java.time.Instant;

/**
 * Internal snapshot of aggregated productivity metrics used to build the AI prompt.
 * Not exposed outside the {@code ai} module.
 */
public record ProductivityAnalysisPromptInput(
        Instant from,
        Instant to,
        ProductivityOverviewResponse overview,
        TaskStatisticsResponse tasks,
        PomodoroStatisticsResponse pomodoro,
        HabitStatisticsResponse habits
) {
}
