package com.lumind.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.auth.dto.request.LoginRequest;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void register_validRequest_returns201WithTokens() throws Exception {
        RegisterRequest request = AuthTestData.validRegisterRequest("register.ok@example.com");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(request.email()))
                .andExpect(jsonPath("$.user.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.user.lastName").value(request.lastName()));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = AuthTestData.validRegisterRequest("duplicate@example.com");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void register_invalidPayload_returns400WithValidationErrors() throws Exception {
        Map<String, String> invalidRequest = Map.of(
                "email", "not-an-email",
                "password", "short",
                "firstName", "",
                "lastName", ""
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("login.ok@example.com");
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(registerRequest.email(), registerRequest.password());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(registerRequest.email()));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("login.fail@example.com");
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest wrongPassword = new LoginRequest(registerRequest.email(), "WrongPass99");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        LoginRequest unknownUser = new LoginRequest("unknown@example.com", AuthTestData.RAW_PASSWORD);
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_disabledAccount_returns403() throws Exception {
        User disabledUser = AuthTestData.activeUser("disabled@example.com");
        disabledUser.setEnabled(false);
        disabledUser.setPassword(passwordEncoder.encode(AuthTestData.RAW_PASSWORD));
        userRepository.save(disabledUser);

        LoginRequest loginRequest = new LoginRequest(disabledUser.getEmail(), AuthTestData.RAW_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void refresh_validRefreshToken_returns200WithNewTokens() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("refresh.ok@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = extractRefreshToken(registerResult);
        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(registerRequest.email()));
    }

    @Test
    void refresh_invalidRefreshToken_returns401() throws Exception {
        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", "invalid.refresh.token"));

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void refresh_disabledAccount_returns403() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("refresh.disabled@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = extractRefreshToken(registerResult);

        User user = userRepository.findByEmail(registerRequest.email()).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    private String extractRefreshToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String refreshToken = json.path("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();
        return refreshToken;
    }
}
