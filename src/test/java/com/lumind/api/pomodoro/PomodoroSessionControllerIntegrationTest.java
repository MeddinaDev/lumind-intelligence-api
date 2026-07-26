package com.lumind.api.pomodoro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.repository.PomodoroSessionRepository;
import com.lumind.api.pomodoro.support.PomodoroSessionTestData;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PomodoroSessionControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String POMODORO_SESSIONS_URL = "/api/v1/pomodoro-sessions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PomodoroSessionRepository pomodoroSessionRepository;

    @BeforeEach
    void cleanDatabase() {
        pomodoroSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.create@example.com");
        CreatePomodoroSessionRequest request = PomodoroSessionTestData.validCreateRequest();

        mockMvc.perform(post(POMODORO_SESSIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.durationMinutes").value(request.durationMinutes()))
                .andExpect(jsonPath("$.completedMinutes").value(0))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.validation@example.com");
        Map<String, Object> invalidRequest = Map.of(
                "durationMinutes", 0,
                "startedAt", Instant.parse("2026-07-14T08:00:00Z")
        );

        mockMvc.perform(post(POMODORO_SESSIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAll_returnsOnlyOwnSessions() throws Exception {
        String userAToken = registerAndGetAccessToken("pomodoro.usera@example.com");
        String userBToken = registerAndGetAccessToken("pomodoro.userb@example.com");

        createSession(userAToken, PomodoroSessionTestData.validCreateRequest(25, Instant.parse("2026-07-14T08:00:00Z")));
        createSession(userAToken, PomodoroSessionTestData.validCreateRequest(50, Instant.parse("2026-07-14T09:00:00Z")));
        createSession(userBToken, PomodoroSessionTestData.validCreateRequest(25, Instant.parse("2026-07-14T10:00:00Z")));

        mockMvc.perform(get(POMODORO_SESSIONS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].durationMinutes").value(25))
                .andExpect(jsonPath("$[1].durationMinutes").value(50));

        mockMvc.perform(get(POMODORO_SESSIONS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].durationMinutes").value(25));
    }

    @Test
    void getById_existingSession_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.get@example.com");
        String sessionId = createSession(accessToken, PomodoroSessionTestData.validCreateRequest());

        mockMvc.perform(get(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.durationMinutes").value(PomodoroSessionTestData.SESSION_DURATION_MINUTES));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.notfound@example.com");
        UUID missingSessionId = UUID.randomUUID();

        mockMvc.perform(get(POMODORO_SESSIONS_URL + "/" + missingSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pomodoro session not found"));
    }

    @Test
    void getById_otherUsersSession_returns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("pomodoro.owner@example.com");
        String otherUserToken = registerAndGetAccessToken("pomodoro.other@example.com");
        String sessionId = createSession(ownerToken, PomodoroSessionTestData.validCreateRequest());

        mockMvc.perform(get(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pomodoro session not found"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.update@example.com");
        String sessionId = createSession(accessToken, PomodoroSessionTestData.validCreateRequest());
        UpdatePomodoroSessionRequest request = PomodoroSessionTestData.validUpdateRequest();

        mockMvc.perform(patch(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.completedMinutes").value(request.completedMinutes()))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.update404@example.com");
        UpdatePomodoroSessionRequest request = PomodoroSessionTestData.validUpdateRequest();

        mockMvc.perform(patch(POMODORO_SESSIONS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pomodoro session not found"));
    }

    @Test
    void delete_existingSession_returns204() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.delete@example.com");
        String sessionId = createSession(accessToken, PomodoroSessionTestData.validCreateRequest());

        mockMvc.perform(delete(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(POMODORO_SESSIONS_URL + "/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pomodoro session not found"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("pomodoro.delete404@example.com");

        mockMvc.perform(delete(POMODORO_SESSIONS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pomodoro session not found"));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get(POMODORO_SESSIONS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
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

    private String createSession(String accessToken, CreatePomodoroSessionRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(POMODORO_SESSIONS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asText();
        assertThat(sessionId).isNotBlank();
        return sessionId;
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
