package com.lumind.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumind.api.auth.dto.request.LoginRequest;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.config.JwtProperties;
import com.lumind.api.support.AbstractIntegrationTest;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String TASKS_URL = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

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
    void refresh_rotatesToken_oldRefreshTokenCannotBeReused() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("refresh.rotate@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String originalRefresh = extractRefreshToken(registerResult);
        MvcResult refreshResult = mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(originalRefresh)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefresh = extractRefreshToken(refreshResult);
        assertThat(newRefresh).isNotEqualTo(originalRefresh);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(originalRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void refresh_reuseOfRevokedToken_revokesRemainingSessions() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("refresh.reuse@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshA = extractRefreshToken(registerResult);
        MvcResult firstRotation = mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshA)))
                .andExpect(status().isOk())
                .andReturn();
        String refreshB = extractRefreshToken(firstRotation);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshA)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshB)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logout_validSession_invalidatesRefreshToken() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("logout.ok@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = extractRefreshToken(registerResult);
        String accessToken = extractAccessToken(registerResult);

        mockMvc.perform(post(LOGOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        mockMvc.perform(get(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_repeatedLogout_isIdempotent() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("logout.repeat@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = extractRefreshToken(registerResult);
        String body = refreshBody(refreshToken);

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(LOGOUT_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void accessToken_validBearer_allowsProtectedResource() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("jwt.valid@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(get(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(extractAccessToken(registerResult))))
                .andExpect(status().isOk());
    }

    @Test
    void accessToken_wrongSigningSecret_returns401() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("jwt.bad-sig@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        User user = userRepository.findByEmail(registerRequest.email()).orElseThrow();
        String tokenWithWrongSecret = signAccessTokenWithSecret(
                user,
                AuthTestData.alternateSecretJwtProperties().secret()
        );

        mockMvc.perform(get(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenWithWrongSecret)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void accessToken_refreshTokenUsedAsBearer_returns401() throws Exception {
        RegisterRequest registerRequest = AuthTestData.validRegisterRequest("jwt.refresh-as-access@example.com");
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(get(TASKS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(extractRefreshToken(registerResult))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
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

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = json.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String signAccessTokenWithSecret(User user, String base64Secret) {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                io.jsonwebtoken.io.Decoders.BASE64.decode(base64Secret)
        );
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.accessTokenExpiration())))
                .signWith(signingKey)
                .compact();
    }
}
