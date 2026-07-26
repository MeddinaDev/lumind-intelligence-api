package com.lumind.api.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lumind.api.ai.client.AiLanguageModelClient;
import com.lumind.api.ai.dto.request.ProductivityAnalysisRequest;
import com.lumind.api.ai.dto.response.ProductivityAnalysisResponse;
import com.lumind.api.ai.prompt.ProductivityAnalysisPromptBuilder;
import com.lumind.api.ai.prompt.model.ProductivityAnalysisPromptInput;
import com.lumind.api.statistics.dto.request.StatisticsPeriodQuery;
import com.lumind.api.statistics.dto.response.HabitStatisticsResponse;
import com.lumind.api.statistics.dto.response.PomodoroStatisticsResponse;
import com.lumind.api.statistics.dto.response.ProductivityOverviewResponse;
import com.lumind.api.statistics.dto.response.TaskStatisticsResponse;
import com.lumind.api.statistics.service.ProductivityStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductivityAnalysisService {

    private static final int MAX_LIST_ITEMS = 10;

    private final ProductivityStatisticsService productivityStatisticsService;
    private final ProductivityAnalysisPromptBuilder productivityAnalysisPromptBuilder;
    private final AiLanguageModelClient aiLanguageModelClient;

    public ProductivityAnalysisService(
            ProductivityStatisticsService productivityStatisticsService,
            ProductivityAnalysisPromptBuilder productivityAnalysisPromptBuilder,
            AiLanguageModelClient aiLanguageModelClient
    ) {
        this.productivityStatisticsService = productivityStatisticsService;
        this.productivityAnalysisPromptBuilder = productivityAnalysisPromptBuilder;
        this.aiLanguageModelClient = aiLanguageModelClient;
    }

    @Transactional(readOnly = true)
    public ProductivityAnalysisResponse analyze(UUID userId, ProductivityAnalysisRequest request) {
        StatisticsPeriodQuery periodQuery = toStatisticsPeriodQuery(request);

        ProductivityOverviewResponse overview = productivityStatisticsService.getOverview(userId, periodQuery);
        TaskStatisticsResponse tasks = productivityStatisticsService.getTaskStatistics(userId, periodQuery);
        PomodoroStatisticsResponse pomodoro = productivityStatisticsService.getPomodoroStatistics(userId, periodQuery);
        HabitStatisticsResponse habits = productivityStatisticsService.getHabitStatistics(userId, periodQuery);

        ProductivityAnalysisPromptInput promptInput = new ProductivityAnalysisPromptInput(
                overview.from(),
                overview.to(),
                overview,
                tasks,
                pomodoro,
                habits
        );

        String prompt = productivityAnalysisPromptBuilder.build(promptInput);
        String rawModelResponse = aiLanguageModelClient.generateCompletion(prompt);
        ParsedAnalysis parsedAnalysis = parseRawModelResponse(rawModelResponse);

        return new ProductivityAnalysisResponse(
                overview.from(),
                overview.to(),
                Instant.now(),
                parsedAnalysis.summary(),
                parsedAnalysis.insights(),
                parsedAnalysis.recommendations()
        );
    }

    private StatisticsPeriodQuery toStatisticsPeriodQuery(ProductivityAnalysisRequest request) {
        return new StatisticsPeriodQuery(request.from(), request.to());
    }

    private ParsedAnalysis parseRawModelResponse(String rawModelResponse) {
        if (!StringUtils.hasText(rawModelResponse)) {
            throw new IllegalStateException("AI model response is empty");
        }

        ObjectMapper objectMapper = JsonMapper.builder().build();
        try {
            JsonNode root = objectMapper.readTree(rawModelResponse.trim());

            String summary = readRequiredText(root, "summary");
            List<String> insights = readRequiredStringList(root, "insights");
            List<String> recommendations = readRequiredStringList(root, "recommendations");

            return new ParsedAnalysis(summary, insights, recommendations);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("AI model response is not valid JSON", ex);
        }
    }

    private String readRequiredText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isTextual()) {
            throw new IllegalStateException("AI model response field '" + fieldName + "' is missing or invalid");
        }

        String value = node.asText().trim();
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("AI model response field '" + fieldName + "' is empty");
        }

        return value;
    }

    private List<String> readRequiredStringList(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("AI model response field '" + fieldName + "' is missing or empty");
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalStateException("AI model response field '" + fieldName + "' contains non-text items");
            }

            String value = item.asText().trim();
            if (!StringUtils.hasText(value)) {
                throw new IllegalStateException("AI model response field '" + fieldName + "' contains blank items");
            }

            values.add(value);
            if (values.size() >= MAX_LIST_ITEMS) {
                break;
            }
        }

        return List.copyOf(values);
    }

    private record ParsedAnalysis(
            String summary,
            List<String> insights,
            List<String> recommendations
    ) {
    }
}
