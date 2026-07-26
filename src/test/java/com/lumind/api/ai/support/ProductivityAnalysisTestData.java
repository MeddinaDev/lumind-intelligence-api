package com.lumind.api.ai.support;

import com.lumind.api.ai.dto.request.ProductivityAnalysisRequest;
import com.lumind.api.ai.dto.response.ProductivityAnalysisResponse;
import com.lumind.api.ai.prompt.model.ProductivityAnalysisPromptInput;
import com.lumind.api.statistics.dto.response.DailyCountResponse;
import com.lumind.api.statistics.dto.response.DailyMinutesResponse;
import com.lumind.api.statistics.dto.response.HabitStatisticsResponse;
import com.lumind.api.statistics.dto.response.PomodoroStatisticsResponse;
import com.lumind.api.statistics.dto.response.ProductivityOverviewResponse;
import com.lumind.api.statistics.dto.response.TaskStatisticsResponse;
import com.lumind.api.statistics.support.ProductivityStatisticsTestData;

import java.time.Instant;
import java.util.List;

public final class ProductivityAnalysisTestData {

    public static final Instant GENERATED_AT = Instant.parse("2026-07-26T12:00:00Z");

    public static final String VALID_MODEL_JSON = """
            {
              "summary": "Resumen de productividad de prueba.",
              "insights": [
                "Se completó el 70% de las tareas planificadas.",
                "El foco Pomodoro fue consistente en la segunda mitad del periodo."
              ],
              "recommendations": [
                "Mantener la rutina de cierre diario de tareas.",
                "Programar bloques Pomodoro en las mañanas con menor actividad."
              ]
            }
            """;

    public static final String INVALID_MODEL_JSON = "not-json";

    public static final String INCOMPLETE_MODEL_JSON = """
            {
              "summary": "Resumen incompleto."
            }
            """;

    private ProductivityAnalysisTestData() {
    }

    public static ProductivityAnalysisRequest emptyPeriodRequest() {
        return new ProductivityAnalysisRequest(null, null);
    }

    public static ProductivityAnalysisRequest customPeriodRequest() {
        return new ProductivityAnalysisRequest(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO
        );
    }

    public static ProductivityAnalysisRequest invalidFromAfterToRequest() {
        return new ProductivityAnalysisRequest(
                ProductivityStatisticsTestData.INVALID_FROM_AFTER_TO_FROM,
                ProductivityStatisticsTestData.INVALID_FROM_AFTER_TO_TO
        );
    }

    public static ProductivityAnalysisResponse sampleAnalysisResponse() {
        return new ProductivityAnalysisResponse(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                GENERATED_AT,
                "Resumen de productividad de prueba.",
                List.of(
                        "Se completó el 70% de las tareas planificadas.",
                        "El foco Pomodoro fue consistente en la segunda mitad del periodo."
                ),
                List.of(
                        "Mantener la rutina de cierre diario de tareas.",
                        "Programar bloques Pomodoro en las mañanas con menor actividad."
                )
        );
    }

    public static ProductivityOverviewResponse sampleOverview() {
        return new ProductivityOverviewResponse(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                new ProductivityOverviewResponse.TaskOverviewMetrics(
                        ProductivityStatisticsTestData.TASKS_CREATED,
                        ProductivityStatisticsTestData.TASKS_COMPLETED,
                        ProductivityStatisticsTestData.expectedTaskCompletionRate()
                ),
                new ProductivityOverviewResponse.PomodoroOverviewMetrics(
                        ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED,
                        ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED,
                        ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES,
                        ProductivityStatisticsTestData.expectedPomodoroCompletionRate()
                ),
                new ProductivityOverviewResponse.HabitOverviewMetrics(
                        ProductivityStatisticsTestData.TOTAL_HABITS,
                        ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD
                )
        );
    }

    public static ProductivityOverviewResponse emptyOverview(Instant from, Instant to) {
        return new ProductivityOverviewResponse(
                from,
                to,
                new ProductivityOverviewResponse.TaskOverviewMetrics(0L, 0L, 0.0),
                new ProductivityOverviewResponse.PomodoroOverviewMetrics(0L, 0L, 0L, 0.0),
                new ProductivityOverviewResponse.HabitOverviewMetrics(0L, 0L)
        );
    }

    public static TaskStatisticsResponse sampleTaskStatistics() {
        return new TaskStatisticsResponse(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                ProductivityStatisticsTestData.TASKS_CREATED,
                ProductivityStatisticsTestData.TASKS_COMPLETED,
                ProductivityStatisticsTestData.TASKS_PENDING_IN_PERIOD,
                ProductivityStatisticsTestData.expectedTaskCompletionRate(),
                List.of(new DailyCountResponse(
                        ProductivityStatisticsTestData.DAILY_COUNT_DATE,
                        ProductivityStatisticsTestData.DAILY_TASK_COUNT
                ))
        );
    }

    public static TaskStatisticsResponse emptyTaskStatistics(Instant from, Instant to) {
        return new TaskStatisticsResponse(from, to, 0L, 0L, 0L, 0.0, List.of());
    }

    public static PomodoroStatisticsResponse samplePomodoroStatistics() {
        return new PomodoroStatisticsResponse(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED,
                ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES,
                ProductivityStatisticsTestData.expectedAverageFocusMinutes(),
                ProductivityStatisticsTestData.expectedPomodoroCompletionRate(),
                List.of(new DailyMinutesResponse(
                        ProductivityStatisticsTestData.DAILY_MINUTES_DATE,
                        ProductivityStatisticsTestData.DAILY_FOCUS_MINUTES
                ))
        );
    }

    public static PomodoroStatisticsResponse emptyPomodoroStatistics(Instant from, Instant to) {
        return new PomodoroStatisticsResponse(from, to, 0L, 0L, 0L, 0L, 0.0, List.of());
    }

    public static HabitStatisticsResponse sampleHabitStatistics() {
        return new HabitStatisticsResponse(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                ProductivityStatisticsTestData.TOTAL_HABITS,
                ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD
        );
    }

    public static HabitStatisticsResponse emptyHabitStatistics(Instant from, Instant to) {
        return new HabitStatisticsResponse(from, to, 0L, 0L);
    }

    public static ProductivityAnalysisPromptInput samplePromptInput() {
        return new ProductivityAnalysisPromptInput(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                sampleOverview(),
                sampleTaskStatistics(),
                samplePomodoroStatistics(),
                sampleHabitStatistics()
        );
    }

    public static ProductivityAnalysisPromptInput emptyPromptInput() {
        Instant from = ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM;
        Instant to = ProductivityStatisticsTestData.CUSTOM_PERIOD_TO;
        return new ProductivityAnalysisPromptInput(
                from,
                to,
                emptyOverview(from, to),
                emptyTaskStatistics(from, to),
                emptyPomodoroStatistics(from, to),
                emptyHabitStatistics(from, to)
        );
    }
}
