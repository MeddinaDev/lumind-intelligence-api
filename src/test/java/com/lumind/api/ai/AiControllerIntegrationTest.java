package com.lumind.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.ai.client.AiLanguageModelClient;
import com.lumind.api.ai.dto.request.ProductivityAnalysisRequest;
import com.lumind.api.ai.exception.AiRateLimitExceededException;
import com.lumind.api.ai.exception.AiRequestTimeoutException;
import com.lumind.api.ai.exception.AiResponseInvalidException;
import com.lumind.api.ai.exception.AiServiceUnavailableException;
import com.lumind.api.ai.support.ProductivityAnalysisTestData;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.repository.HabitRepository;
import com.lumind.api.habit.support.HabitTestData;
import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.repository.PomodoroSessionRepository;
import com.lumind.api.pomodoro.support.PomodoroSessionTestData;
import com.lumind.api.statistics.support.ProductivityStatisticsTestData;
import com.lumind.api.task.dto.request.CreateTaskRequest;
import com.lumind.api.task.dto.request.UpdateTaskRequest;
import com.lumind.api.task.repository.TaskRepository;
import com.lumind.api.task.support.TaskTestData;
import com.lumind.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String AI_ANALYSIS_URL = "/api/v1/ai/productivity-analysis";
    private static final String HABITS_URL = "/api/v1/habits";
    private static final String TASKS_URL = "/api/v1/tasks";
    private static final String POMODORO_SESSIONS_URL = "/api/v1/pomodoro-sessions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PomodoroSessionRepository pomodoroSessionRepository;

    @MockBean
    private AiLanguageModelClient aiLanguageModelClient;

    @BeforeEach
    void cleanDatabase() {
        pomodoroSessionRepository.deleteAll();
        taskRepository.deleteAll();
        habitRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void generateProductivityAnalysis_withMetrics_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.metrics@example.com");
        seedProductivityData(accessToken);
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resumen de productividad de prueba."))
                .andExpect(jsonPath("$.insights.length()").value(2))
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    void generateProductivityAnalysis_invalidPeriod_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.invalid@example.com");

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ProductivityAnalysisTestData.invalidFromAfterToRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid statistics period"));
    }

    @Test
    void generateProductivityAnalysis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void generateProductivityAnalysis_rateLimitExceeded_returns429() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.ratelimit@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenThrow(new AiRateLimitExceededException());

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("AI analysis rate limit exceeded"));
    }

    @Test
    void generateProductivityAnalysis_invalidAiResponse_returns502() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.invalid-response@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenThrow(new AiResponseInvalidException());

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("AI analysis could not be processed"));
    }

    @Test
    void generateProductivityAnalysis_serviceUnavailable_returns503() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.unavailable@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenThrow(new AiServiceUnavailableException());

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI analysis service is temporarily unavailable"));
    }

    @Test
    void generateProductivityAnalysis_requestTimeout_returns504() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.timeout@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenThrow(new AiRequestTimeoutException());

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.message").value("AI analysis request timed out"));
    }

    @Test
    void generateProductivityAnalysis_userWithoutData_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.empty@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductivityAnalysisTestData.emptyPeriodRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resumen de productividad de prueba."))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void generateProductivityAnalysis_defaultPeriod_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.default@example.com");
        seedProductivityData(accessToken);
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        MvcResult result = mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Instant from = Instant.parse(json.path("from").asText());
        assertThat(from).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
    }

    @Test
    void generateProductivityAnalysis_customPeriod_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("ai.custom@example.com");
        when(aiLanguageModelClient.generateCompletion(anyString()))
                .thenReturn(ProductivityAnalysisTestData.VALID_MODEL_JSON);

        ProductivityAnalysisRequest request = ProductivityAnalysisTestData.customPeriodRequest();

        mockMvc.perform(post(AI_ANALYSIS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM.toString()))
                .andExpect(jsonPath("$.to").value(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO.toString()))
                .andExpect(jsonPath("$.summary").value("Resumen de productividad de prueba."));
    }

    private void seedProductivityData(String accessToken) throws Exception {
        createHabit(accessToken, HabitTestData.validCreateRequest("Morning run", "Daily run"));
        createHabit(accessToken, HabitTestData.validCreateRequest("Reading", "Read 20 minutes"));

        String completedTaskId = createTask(accessToken, TaskTestData.validCreateRequest("Complete report", "Q2"));
        createTask(accessToken, TaskTestData.validCreateRequest("Review PRs", "Open PRs"));
        completeTask(accessToken, completedTaskId, TaskTestData.validUpdateRequest());

        Instant completedStartedAt = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant completedFinishedAt = completedStartedAt.plus(25, ChronoUnit.MINUTES);
        String completedSessionId = createPomodoroSession(
                accessToken,
                PomodoroSessionTestData.validCreateRequest(25, completedStartedAt)
        );
        createPomodoroSession(
                accessToken,
                PomodoroSessionTestData.validCreateRequest(50, Instant.now().minus(1, ChronoUnit.HOURS))
        );
        completePomodoroSession(
                accessToken,
                completedSessionId,
                new UpdatePomodoroSessionRequest(25, true, completedFinishedAt)
        );
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        RegisterRequest request = AuthTestData.validRegisterRequest(email);
        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractAccessToken(result);
    }

    private void createHabit(String accessToken, CreateHabitRequest request) throws Exception {
        mockMvc.perform(post(HABITS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String createTask(String accessToken, CreateTaskRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(taskId).isNotBlank();
        return taskId;
    }

    private void completeTask(String accessToken, String taskId, UpdateTaskRequest request) throws Exception {
        mockMvc.perform(patch(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private String createPomodoroSession(String accessToken, CreatePomodoroSessionRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(POMODORO_SESSIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(sessionId).isNotBlank();
        return sessionId;
    }

    private void completePomodoroSession(
            String accessToken,
            String sessionId,
            UpdatePomodoroSessionRequest request
    ) throws Exception {
        mockMvc.perform(patch(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = json.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
