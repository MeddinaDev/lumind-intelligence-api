package com.lumind.api.habit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.repository.HabitRepository;
import com.lumind.api.habit.support.HabitTestData;
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
class HabitControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String HABITS_URL = "/api/v1/habits";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @BeforeEach
    void cleanDatabase() {
        habitRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.create@example.com");
        CreateHabitRequest request = HabitTestData.validCreateRequest();

        mockMvc.perform(post(HABITS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.validation@example.com");
        Map<String, String> invalidRequest = Map.of("name", "", "description", "x".repeat(501));

        mockMvc.perform(post(HABITS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAll_returnsOnlyOwnHabits() throws Exception {
        String userAToken = registerAndGetAccessToken("habit.usera@example.com");
        String userBToken = registerAndGetAccessToken("habit.userb@example.com");

        createHabit(userAToken, HabitTestData.validCreateRequest("Habit A1", "First"));
        createHabit(userAToken, HabitTestData.validCreateRequest("Habit A2", "Second"));
        createHabit(userBToken, HabitTestData.validCreateRequest("Habit B1", "Other user"));

        mockMvc.perform(get(HABITS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Habit A1"))
                .andExpect(jsonPath("$[1].name").value("Habit A2"));

        mockMvc.perform(get(HABITS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Habit B1"));
    }

    @Test
    void getById_existingHabit_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.get@example.com");
        String habitId = createHabit(accessToken, HabitTestData.validCreateRequest());

        mockMvc.perform(get(HABITS_URL + "/" + habitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(habitId))
                .andExpect(jsonPath("$.name").value(HabitTestData.HABIT_NAME));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.notfound@example.com");
        UUID missingHabitId = UUID.randomUUID();

        mockMvc.perform(get(HABITS_URL + "/" + missingHabitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Habit not found"));
    }

    @Test
    void getById_otherUsersHabit_returns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("habit.owner@example.com");
        String otherUserToken = registerAndGetAccessToken("habit.other@example.com");
        String habitId = createHabit(ownerToken, HabitTestData.validCreateRequest());

        mockMvc.perform(get(HABITS_URL + "/" + habitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Habit not found"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.update@example.com");
        String habitId = createHabit(accessToken, HabitTestData.validCreateRequest());
        UpdateHabitRequest request = HabitTestData.validUpdateRequest();

        mockMvc.perform(patch(HABITS_URL + "/" + habitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(habitId))
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.update404@example.com");
        UpdateHabitRequest request = HabitTestData.validUpdateRequest();

        mockMvc.perform(patch(HABITS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Habit not found"));
    }

    @Test
    void delete_existingHabit_returns204() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.delete@example.com");
        String habitId = createHabit(accessToken, HabitTestData.validCreateRequest());

        mockMvc.perform(delete(HABITS_URL + "/" + habitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(HABITS_URL + "/" + habitId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Habit not found"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("habit.delete404@example.com");

        mockMvc.perform(delete(HABITS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Habit not found"));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get(HABITS_URL))
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

    private String createHabit(String accessToken, CreateHabitRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(HABITS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String habitId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asText();
        assertThat(habitId).isNotBlank();
        return habitId;
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
