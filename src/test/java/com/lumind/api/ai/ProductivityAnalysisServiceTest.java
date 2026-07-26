package com.lumind.api.ai;

import com.lumind.api.ai.client.AiLanguageModelClient;
import com.lumind.api.ai.dto.request.ProductivityAnalysisRequest;
import com.lumind.api.ai.dto.response.ProductivityAnalysisResponse;
import com.lumind.api.ai.exception.AiConfigurationException;
import com.lumind.api.ai.exception.AiRateLimitExceededException;
import com.lumind.api.ai.exception.AiRequestTimeoutException;
import com.lumind.api.ai.exception.AiServiceUnavailableException;
import com.lumind.api.ai.prompt.ProductivityAnalysisPromptBuilder;
import com.lumind.api.ai.prompt.model.ProductivityAnalysisPromptInput;
import com.lumind.api.ai.service.ProductivityAnalysisService;
import com.lumind.api.ai.support.ProductivityAnalysisTestData;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.statistics.dto.request.StatisticsPeriodQuery;
import com.lumind.api.statistics.exception.InvalidStatisticsPeriodException;
import com.lumind.api.statistics.service.ProductivityStatisticsService;
import com.lumind.api.statistics.support.ProductivityStatisticsTestData;
import com.lumind.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductivityAnalysisServiceTest {

    @Mock
    private ProductivityStatisticsService productivityStatisticsService;

    @Mock
    private ProductivityAnalysisPromptBuilder productivityAnalysisPromptBuilder;

    @Mock
    private AiLanguageModelClient aiLanguageModelClient;

    @InjectMocks
    private ProductivityAnalysisService productivityAnalysisService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        userId = user.getId();
    }

    @Test
    void analyze_defaultPeriod_returnsResponseAndCallsStatisticsWithEmptyQuery() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.emptyPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        ProductivityAnalysisResponse response = productivityAnalysisService.analyze(userId, request);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        assertThat(response.summary()).isEqualTo("Resumen de productividad de prueba.");
        assertThat(response.insights()).hasSize(2);
        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.generatedAt()).isNotNull();

        ArgumentCaptor<StatisticsPeriodQuery> queryCaptor = ArgumentCaptor.forClass(StatisticsPeriodQuery.class);
        verify(productivityStatisticsService).getOverview(eq(userId), queryCaptor.capture());
        assertThat(queryCaptor.getValue().from()).isNull();
        assertThat(queryCaptor.getValue().to()).isNull();
    }

    @Test
    void analyze_customPeriod_returnsResponseWithPeriodBounds() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        ProductivityAnalysisResponse response = productivityAnalysisService.analyze(userId, request);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);

        verify(productivityStatisticsService).getOverview(
                userId,
                new StatisticsPeriodQuery(
                        ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                        ProductivityStatisticsTestData.CUSTOM_PERIOD_TO
                )
        );
    }

    @Test
    void analyze_fullFlow_invokesStatisticsPromptBuilderAndClient() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("generated-prompt");
        when(aiLanguageModelClient.generateCompletion("generated-prompt"))
                .thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        productivityAnalysisService.analyze(userId, request);

        verify(productivityStatisticsService).getTaskStatistics(eq(userId), any(StatisticsPeriodQuery.class));
        verify(productivityStatisticsService).getPomodoroStatistics(eq(userId), any(StatisticsPeriodQuery.class));
        verify(productivityStatisticsService).getHabitStatistics(eq(userId), any(StatisticsPeriodQuery.class));

        ArgumentCaptor<ProductivityAnalysisPromptInput> promptInputCaptor =
                ArgumentCaptor.forClass(ProductivityAnalysisPromptInput.class);
        verify(productivityAnalysisPromptBuilder).build(promptInputCaptor.capture());

        ProductivityAnalysisPromptInput promptInput = promptInputCaptor.getValue();
        assertThat(promptInput.overview()).isEqualTo(ProductivityAnalysisTestData.sampleOverview());
        assertThat(promptInput.tasks()).isEqualTo(ProductivityAnalysisTestData.sampleTaskStatistics());
        assertThat(promptInput.pomodoro()).isEqualTo(ProductivityAnalysisTestData.samplePomodoroStatistics());
        assertThat(promptInput.habits()).isEqualTo(ProductivityAnalysisTestData.sampleHabitStatistics());

        verify(aiLanguageModelClient).generateCompletion("generated-prompt");
    }

    @Test
    void analyze_validJsonResponse_parsesAllFields() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        ProductivityAnalysisResponse response = productivityAnalysisService.analyze(userId, request);

        assertThat(response.summary()).isEqualTo("Resumen de productividad de prueba.");
        assertThat(response.insights()).containsExactly(
                "Se completó el 70% de las tareas planificadas.",
                "El foco Pomodoro fue consistente en la segunda mitad del periodo."
        );
        assertThat(response.recommendations()).containsExactly(
                "Mantener la rutina de cierre diario de tareas.",
                "Programar bloques Pomodoro en las mañanas con menor actividad."
        );
    }

    @Test
    void analyze_invalidJson_throwsIllegalStateException() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt"))
                .thenReturn(ProductivityAnalysisTestData.INVALID_MODEL_JSON);

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void analyze_incompleteResponse_throwsIllegalStateException() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt"))
                .thenReturn(ProductivityAnalysisTestData.INCOMPLETE_MODEL_JSON);

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insights");
    }

    @Test
    void analyze_invalidPeriod_propagatesInvalidStatisticsPeriodException() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.invalidFromAfterToRequest();
        when(productivityStatisticsService.getOverview(eq(userId), any(StatisticsPeriodQuery.class)))
                .thenThrow(new InvalidStatisticsPeriodException());

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(InvalidStatisticsPeriodException.class);

        verify(productivityAnalysisPromptBuilder, never()).build(any());
        verify(aiLanguageModelClient, never()).generateCompletion(anyString());
    }

    @Test
    void analyze_noData_returnsParsedResponse() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        var overview = ProductivityAnalysisTestData.emptyOverview(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO
        );
        stubStatisticsResponses(overview);
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        ProductivityAnalysisResponse response = productivityAnalysisService.analyze(userId, request);

        assertThat(response.summary()).isNotBlank();
        assertThat(response.insights()).isNotEmpty();
        assertThat(response.recommendations()).isNotEmpty();
    }

    @Test
    void analyze_aiServiceUnavailableException_propagates() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenThrow(new AiServiceUnavailableException());

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void analyze_aiRequestTimeoutException_propagates() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenThrow(new AiRequestTimeoutException());

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(AiRequestTimeoutException.class);
    }

    @Test
    void analyze_aiRateLimitExceededException_propagates() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenThrow(new AiRateLimitExceededException());

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(AiRateLimitExceededException.class);
    }

    @Test
    void analyze_aiConfigurationException_propagates() {
        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();
        stubStatisticsResponses(ProductivityAnalysisTestData.sampleOverview());
        when(productivityAnalysisPromptBuilder.build(any())).thenReturn("prompt");
        when(aiLanguageModelClient.generateCompletion("prompt")).thenThrow(new AiConfigurationException());

        assertThatThrownBy(() -> productivityAnalysisService.analyze(userId, request))
                .isInstanceOf(AiConfigurationException.class);
    }

    private void stubStatisticsResponses(
            com.lumind.api.statistics.dto.response.ProductivityOverviewResponse overview
    ) {
        when(productivityStatisticsService.getOverview(eq(userId), any(StatisticsPeriodQuery.class)))
                .thenReturn(overview);
        when(productivityStatisticsService.getTaskStatistics(eq(userId), any(StatisticsPeriodQuery.class)))
                .thenReturn(ProductivityAnalysisTestData.sampleTaskStatistics());
        when(productivityStatisticsService.getPomodoroStatistics(eq(userId), any(StatisticsPeriodQuery.class)))
                .thenReturn(ProductivityAnalysisTestData.samplePomodoroStatistics());
        when(productivityStatisticsService.getHabitStatistics(eq(userId), any(StatisticsPeriodQuery.class)))
                .thenReturn(ProductivityAnalysisTestData.sampleHabitStatistics());
    }
}
