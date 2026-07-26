package com.lumind.api.ai.prompt;

import com.lumind.api.ai.prompt.model.ProductivityAnalysisPromptInput;
import com.lumind.api.ai.support.ProductivityAnalysisTestData;
import com.lumind.api.statistics.support.ProductivityStatisticsTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductivityAnalysisPromptBuilderTest {

    private ProductivityAnalysisPromptBuilder productivityAnalysisPromptBuilder;

    @BeforeEach
    void setUp() {
        productivityAnalysisPromptBuilder = new ProductivityAnalysisPromptBuilder();
    }

    @Test
    void build_includesAggregatedMetrics() {
        ProductivityAnalysisPromptInput input = ProductivityAnalysisTestData.samplePromptInput();

        String prompt = productivityAnalysisPromptBuilder.build(input);

        assertThat(prompt).contains("\"overview\"");
        assertThat(prompt).contains("\"tasks\"");
        assertThat(prompt).contains("\"pomodoro\"");
        assertThat(prompt).contains("\"habits\"");
        assertThat(prompt).contains(String.valueOf(ProductivityStatisticsTestData.TASKS_CREATED));
        assertThat(prompt).contains(String.valueOf(ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES));
        assertThat(prompt).contains(String.valueOf(ProductivityStatisticsTestData.TOTAL_HABITS));
    }

    @Test
    void build_excludesPii() {
        ProductivityAnalysisPromptInput input = ProductivityAnalysisTestData.samplePromptInput();

        String prompt = productivityAnalysisPromptBuilder.build(input);

        assertThat(prompt).doesNotContain("@example.com");
        assertThat(prompt).doesNotContain("user@");
        assertThat(prompt).doesNotContain("Morning run");
        assertThat(prompt).doesNotContain("Complete report");
    }

    @Test
    void build_includesRequiredJsonOutputSchema() {
        ProductivityAnalysisPromptInput input = ProductivityAnalysisTestData.samplePromptInput();

        String prompt = productivityAnalysisPromptBuilder.build(input);

        assertThat(prompt).contains("\"summary\"");
        assertThat(prompt).contains("\"insights\"");
        assertThat(prompt).contains("\"recommendations\"");
        assertThat(prompt).contains("Return ONLY valid JSON");
    }

    @Test
    void build_withEmptyMetrics_stillGeneratesPrompt() {
        ProductivityAnalysisPromptInput input = ProductivityAnalysisTestData.emptyPromptInput();

        String prompt = productivityAnalysisPromptBuilder.build(input);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("If all metrics are zero");
        assertThat(prompt).contains("\"created\" : 0");
        assertThat(prompt).contains("\"totalHabits\" : 0");
    }

    @Test
    void build_includesAnalysisPeriodContext() {
        ProductivityAnalysisPromptInput input = ProductivityAnalysisTestData.samplePromptInput();

        String prompt = productivityAnalysisPromptBuilder.build(input);

        assertThat(prompt).contains(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM.toString());
        assertThat(prompt).contains(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO.toString());
        assertThat(prompt).contains("durationDays");
    }
}
