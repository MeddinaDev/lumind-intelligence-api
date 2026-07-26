package com.lumind.api.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatisticsControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String STATISTICS_URL = "/api/v1/statistics";
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

    @BeforeEach
    void cleanDatabase() {
        pomodoroSessionRepository.deleteAll();
        taskRepository.deleteAll();
        habitRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getOverview_withMetrics_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.overview@example.com");
        seedProductivityData(accessToken);

        mockMvc.perform(get(STATISTICS_URL + "/overview?" + ProductivityStatisticsTestData.recentPeriodQueryParams())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.created").value(2))
                .andExpect(jsonPath("$.tasks.completed").value(1))
                .andExpect(jsonPath("$.tasks.completionRate").value(0.5))
                .andExpect(jsonPath("$.pomodoroSessions.sessionsStarted").value(2))
                .andExpect(jsonPath("$.pomodoroSessions.sessionsCompleted").value(1))
                .andExpect(jsonPath("$.pomodoroSessions.totalFocusMinutes").value(25))
                .andExpect(jsonPath("$.pomodoroSessions.completionRate").value(0.5))
                .andExpect(jsonPath("$.habits.totalHabits").value(2))
                .andExpect(jsonPath("$.habits.createdInPeriod").value(2));
    }

    @Test
    void getTasks_withMetrics_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.tasks@example.com");
        seedProductivityData(accessToken);

        mockMvc.perform(get(STATISTICS_URL + "/tasks?" + ProductivityStatisticsTestData.recentPeriodQueryParams())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.pendingCreatedInPeriod").value(1))
                .andExpect(jsonPath("$.completionRate").value(0.5))
                .andExpect(jsonPath("$.completedByDay").isArray())
                .andExpect(jsonPath("$.completedByDay.length()").value(1));
    }

    @Test
    void getPomodoroSessions_withMetrics_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.pomodoro@example.com");
        seedProductivityData(accessToken);

        mockMvc.perform(get(STATISTICS_URL + "/pomodoro-sessions?" + ProductivityStatisticsTestData.recentPeriodQueryParams())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsStarted").value(2))
                .andExpect(jsonPath("$.sessionsCompleted").value(1))
                .andExpect(jsonPath("$.totalFocusMinutes").value(25))
                .andExpect(jsonPath("$.averageFocusMinutesPerCompletedSession").value(25))
                .andExpect(jsonPath("$.completionRate").value(0.5))
                .andExpect(jsonPath("$.focusMinutesByDay").isArray())
                .andExpect(jsonPath("$.focusMinutesByDay.length()").value(1));
    }

    @Test
    void getHabits_withMetrics_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.habits@example.com");
        seedProductivityData(accessToken);

        mockMvc.perform(get(STATISTICS_URL + "/habits?" + ProductivityStatisticsTestData.recentPeriodQueryParams())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHabits").value(2))
                .andExpect(jsonPath("$.createdInPeriod").value(2));
    }

    @Test
    void getOverview_defaultPeriod_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.default@example.com");
        seedProductivityData(accessToken);

        MvcResult result = mockMvc.perform(get(STATISTICS_URL + "/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty())
                .andExpect(jsonPath("$.tasks.created").value(2))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Instant from = Instant.parse(json.path("from").asText());
        assertThat(from).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
    }

    @Test
    void getOverview_customPeriod_returnsMatchingBounds() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.custom@example.com");

        mockMvc.perform(get(STATISTICS_URL + "/overview?" + ProductivityStatisticsTestData.customPeriodQueryParams())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM.toString()))
                .andExpect(jsonPath("$.to").value(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO.toString()))
                .andExpect(jsonPath("$.tasks.created").value(0))
                .andExpect(jsonPath("$.habits.totalHabits").value(0));
    }

    @Test
    void userWithoutData_returnsZeroMetricsOnAllEndpoints() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.empty@example.com");
        String queryParams = ProductivityStatisticsTestData.recentPeriodQueryParams();

        mockMvc.perform(get(STATISTICS_URL + "/overview?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.created").value(0))
                .andExpect(jsonPath("$.tasks.completed").value(0))
                .andExpect(jsonPath("$.tasks.completionRate").value(0.0))
                .andExpect(jsonPath("$.pomodoroSessions.sessionsStarted").value(0))
                .andExpect(jsonPath("$.habits.totalHabits").value(0));

        mockMvc.perform(get(STATISTICS_URL + "/tasks?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.completedByDay").isEmpty());

        mockMvc.perform(get(STATISTICS_URL + "/pomodoro-sessions?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsStarted").value(0))
                .andExpect(jsonPath("$.focusMinutesByDay").isEmpty());

        mockMvc.perform(get(STATISTICS_URL + "/habits?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHabits").value(0))
                .andExpect(jsonPath("$.createdInPeriod").value(0));
    }

    @Test
    void userIsolation_returnsOnlyOwnMetricsOnAllEndpoints() throws Exception {
        String userAToken = registerAndGetAccessToken("statistics.usera@example.com");
        String userBToken = registerAndGetAccessToken("statistics.userb@example.com");

        createHabit(userAToken, HabitTestData.validCreateRequest("Habit A1", "First"));
        createHabit(userAToken, HabitTestData.validCreateRequest("Habit A2", "Second"));
        createHabit(userBToken, HabitTestData.validCreateRequest("Habit B1", "Other user"));

        createTask(userAToken, TaskTestData.validCreateRequest("Task A1", "First"));
        createTask(userAToken, TaskTestData.validCreateRequest("Task A2", "Second"));
        createTask(userBToken, TaskTestData.validCreateRequest("Task B1", "Other user"));

        String queryParams = ProductivityStatisticsTestData.recentPeriodQueryParams();

        mockMvc.perform(get(STATISTICS_URL + "/overview?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.created").value(2))
                .andExpect(jsonPath("$.habits.totalHabits").value(2));

        mockMvc.perform(get(STATISTICS_URL + "/overview?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.created").value(1))
                .andExpect(jsonPath("$.habits.totalHabits").value(1));

        mockMvc.perform(get(STATISTICS_URL + "/tasks?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2));

        mockMvc.perform(get(STATISTICS_URL + "/tasks?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1));

        mockMvc.perform(get(STATISTICS_URL + "/habits?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHabits").value(2));

        mockMvc.perform(get(STATISTICS_URL + "/habits?" + queryParams)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHabits").value(1));
    }

    @Test
    void invalidPeriod_returns400OnAllEndpoints() throws Exception {
        String accessToken = registerAndGetAccessToken("statistics.invalid@example.com");
        String fromAfterToParams = ProductivityStatisticsTestData.invalidFromAfterToQueryParams();
        String periodTooLongParams = ProductivityStatisticsTestData.periodTooLongQueryParams();

        for (String endpoint : new String[] {"/overview", "/tasks", "/pomodoro-sessions", "/habits"}) {
            mockMvc.perform(get(STATISTICS_URL + endpoint + "?" + fromAfterToParams)
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid statistics period"));

            mockMvc.perform(get(STATISTICS_URL + endpoint + "?" + periodTooLongParams)
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid statistics period"));
        }
    }

    @Test
    void unauthenticatedRequest_returns401OnAllEndpoints() throws Exception {
        for (String endpoint : new String[] {"/overview", "/tasks", "/pomodoro-sessions", "/habits"}) {
            mockMvc.perform(get(STATISTICS_URL + endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }
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
