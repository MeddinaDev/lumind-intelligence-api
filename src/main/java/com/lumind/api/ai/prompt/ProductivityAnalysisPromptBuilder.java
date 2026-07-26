package com.lumind.api.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lumind.api.ai.prompt.model.ProductivityAnalysisPromptInput;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the prompt sent to the language model from aggregated productivity statistics only.
 */
@Component
public class ProductivityAnalysisPromptBuilder {

    private static final String OUTPUT_SCHEMA = """
            {
              "summary": "<2-4 sentence narrative summary in Spanish>",
              "insights": ["<observation 1>", "<observation 2>"],
              "recommendations": ["<action 1>", "<action 2>"]
            }
            """;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public String build(ProductivityAnalysisPromptInput input) {
        long periodDays = ChronoUnit.DAYS.between(
                input.from().atZone(ZoneOffset.UTC).toLocalDate(),
                input.to().atZone(ZoneOffset.UTC).toLocalDate()
        );

        return """
                You are a professional productivity coach analyzing aggregated metrics from a personal \
                productivity application. Respond in Spanish with a professional and concise tone.

                ## Data limitations
                - Task completion dates use updatedAt as a proxy; there is no completedAt field.
                - Habit completion and streak metrics are not available; only inventory and creations in period.
                - Base your analysis exclusively on the metrics provided below. Do not invent data.
                - If all metrics are zero, acknowledge the lack of activity and suggest constructive next steps.

                ## Analysis period
                - from (UTC): %s
                - to (UTC): %s
                - durationDays: %d

                ## Aggregated metrics (JSON)
                %s

                ## Required output format
                Return ONLY valid JSON matching this schema (no markdown fences, no extra text):
                %s

                ## Output rules
                - summary: 2-4 sentences in Spanish.
                - insights: ordered list of concrete observations derived from the metrics.
                - recommendations: ordered list of actionable suggestions.
                - Do not include personal identifiable information.
                """.formatted(
                input.from(),
                input.to(),
                periodDays,
                serializeMetrics(input),
                OUTPUT_SCHEMA
        );
    }

    private String serializeMetrics(ProductivityAnalysisPromptInput input) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("overview", input.overview());
        metrics.put("tasks", input.tasks());
        metrics.put("pomodoro", input.pomodoro());
        metrics.put("habits", input.habits());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize productivity metrics for AI prompt", ex);
        }
    }
}
