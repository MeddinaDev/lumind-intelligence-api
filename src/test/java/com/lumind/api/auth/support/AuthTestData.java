package com.lumind.api.auth.support;

import com.lumind.api.auth.dto.request.LoginRequest;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.dto.response.AuthResponse;
import com.lumind.api.auth.entity.RefreshToken;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.config.JwtProperties;
import com.lumind.api.user.dto.response.UserSummaryResponse;
import com.lumind.api.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class AuthTestData {

    public static final String JWT_SECRET = "MDEyMzQ1Njc4OUFCRUNERjAxMjM0NTY3ODlBQkNERUY=";
    public static final String JWT_ISSUER = "lumind-intelligence-api-test";
    public static final String RAW_PASSWORD = "SecurePass123";
    public static final String TEST_EMAIL = "test@example.com";

    private AuthTestData() {
    }

    public static JwtProperties defaultJwtProperties() {
        return new JwtProperties(JWT_SECRET, JWT_ISSUER, 900L, 604_800L);
    }

    public static JwtProperties shortLivedJwtProperties() {
        return new JwtProperties(JWT_SECRET, JWT_ISSUER, 1L, 1L);
    }

    public static JwtProperties alternateIssuerJwtProperties() {
        return new JwtProperties(JWT_SECRET, "other-issuer", 900L, 604_800L);
    }

    public static User activeUser() {
        return activeUser(TEST_EMAIL);
    }

    public static User activeUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword("$2a$10$encoded-password-hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    public static User disabledUser() {
        User user = activeUser();
        user.setEnabled(false);
        return user;
    }

    public static RegisterRequest validRegisterRequest() {
        return new RegisterRequest("new.user@example.com", RAW_PASSWORD, "María", "García");
    }

    public static RegisterRequest validRegisterRequest(String email) {
        return new RegisterRequest(email, RAW_PASSWORD, "María", "García");
    }

    public static LoginRequest validLoginRequest() {
        return new LoginRequest(TEST_EMAIL, RAW_PASSWORD);
    }

    public static IssuedTokens sampleIssuedTokens() {
        return new IssuedTokens("access-token-value", "refresh-token-value");
    }

    public static AuthResponse sampleAuthResponse(User user) {
        UserSummaryResponse userSummary = new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
        return new AuthResponse(
                "access-token-value",
                "refresh-token-value",
                "Bearer",
                900L,
                userSummary
        );
    }

    public static RefreshToken storedRefreshToken(User user, String rawRefreshToken, boolean revoked) {
        RefreshToken stored = new RefreshToken();
        stored.setId(UUID.randomUUID());
        stored.setUser(user);
        stored.setToken(com.lumind.api.common.util.Sha256Hasher.hashToHex(rawRefreshToken));
        stored.setExpiresAt(Instant.now().plusSeconds(3_600));
        stored.setRevoked(revoked);
        stored.setCreatedAt(Instant.now());
        return stored;
    }

    public static Claims refreshClaims(User user) {
        return refreshClaims(user, Instant.now().plusSeconds(3_600));
    }

    public static Claims refreshClaims(User user, Instant expiration) {
        return Jwts.claims()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuer(JWT_ISSUER)
                .add("type", "refresh")
                .expiration(Date.from(expiration))
                .build();
    }
}
