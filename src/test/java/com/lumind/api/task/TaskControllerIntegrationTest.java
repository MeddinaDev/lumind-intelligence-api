package com.lumind.api.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
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
class TaskControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String TASKS_URL = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        String accessToken = registerAndGetAccessToken("task.create@example.com");
        CreateTaskRequest request = TaskTestData.validCreateRequest();

        mockMvc.perform(post(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        String accessToken = registerAndGetAccessToken("task.validation@example.com");
        Map<String, String> invalidRequest = Map.of("title", "", "description", "x".repeat(501));

        mockMvc.perform(post(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAll_returnsOnlyOwnTasks() throws Exception {
        String userAToken = registerAndGetAccessToken("task.usera@example.com");
        String userBToken = registerAndGetAccessToken("task.userb@example.com");

        createTask(userAToken, TaskTestData.validCreateRequest("Task A1", "First"));
        createTask(userAToken, TaskTestData.validCreateRequest("Task A2", "Second"));
        createTask(userBToken, TaskTestData.validCreateRequest("Task B1", "Other user"));

        mockMvc.perform(get(TASKS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Task A1"))
                .andExpect(jsonPath("$[1].title").value("Task A2"));

        mockMvc.perform(get(TASKS_URL).header(HttpHeaders.AUTHORIZATION, bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Task B1"));
    }

    @Test
    void getById_existingTask_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("task.get@example.com");
        String taskId = createTask(accessToken, TaskTestData.validCreateRequest());

        mockMvc.perform(get(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value(TaskTestData.TASK_TITLE));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("task.notfound@example.com");
        UUID missingTaskId = UUID.randomUUID();

        mockMvc.perform(get(TASKS_URL + "/" + missingTaskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void getById_otherUsersTask_returns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("task.owner@example.com");
        String otherUserToken = registerAndGetAccessToken("task.other@example.com");
        String taskId = createTask(ownerToken, TaskTestData.validCreateRequest());

        mockMvc.perform(get(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("task.update@example.com");
        String taskId = createTask(accessToken, TaskTestData.validCreateRequest());
        UpdateTaskRequest request = TaskTestData.validUpdateRequest();

        mockMvc.perform(patch(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("task.update404@example.com");
        UpdateTaskRequest request = TaskTestData.validUpdateRequest();

        mockMvc.perform(patch(TASKS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void delete_existingTask_returns204() throws Exception {
        String accessToken = registerAndGetAccessToken("task.delete@example.com");
        String taskId = createTask(accessToken, TaskTestData.validCreateRequest());

        mockMvc.perform(delete(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TASKS_URL + "/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        String accessToken = registerAndGetAccessToken("task.delete404@example.com");

        mockMvc.perform(delete(TASKS_URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get(TASKS_URL))
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

    private String createTask(String accessToken, CreateTaskRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asText();
        assertThat(taskId).isNotBlank();
        return taskId;
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
