package com.lumind.api.auth;

import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.config.JwtProperties;
import com.lumind.api.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(AuthTestData.defaultJwtProperties());
        user = AuthTestData.activeUser();
    }

    @Test
    void generateAccessToken_returnsSignedTokenWithUserClaims() {
        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.parseAndValidateAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
        assertThat(claims.getIssuer()).isEqualTo(AuthTestData.JWT_ISSUER);
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    @Test
    void generateRefreshToken_returnsSignedTokenWithRefreshTypeAndJti() {
        String token = jwtService.generateRefreshToken(user);

        Claims claims = jwtService.parseAndValidateRefreshToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuer()).isEqualTo(AuthTestData.JWT_ISSUER);
    }

    @Test
    void extractUserId_returnsUuidFromSubjectClaim() {
        String token = jwtService.generateAccessToken(user);

        UUID userId = jwtService.extractUserId(jwtService.parseAndValidateAccessToken(token));

        assertThat(userId).isEqualTo(user.getId());
    }

    @Test
    void refreshTokenJti_isUniquePerToken() {
        String firstToken = jwtService.generateRefreshToken(user);
        String secondToken = jwtService.generateRefreshToken(user);

        Claims firstClaims = jwtService.parseAndValidateRefreshToken(firstToken);
        Claims secondClaims = jwtService.parseAndValidateRefreshToken(secondToken);

        assertThat(firstClaims.getId()).isNotEqualTo(secondClaims.getId());
    }

    @Test
    void parseAndValidateAccessToken_rejectsRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Refresh token cannot be used as access token");
    }

    @Test
    void parseAndValidateRefreshToken_rejectsAccessToken() {
        String accessToken = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAndValidateRefreshToken(accessToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token is not a refresh token");
    }

    @Test
    void parseAndValidateAccessToken_rejectsExpiredToken() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(AuthTestData.shortLivedJwtProperties());
        String token = shortLivedJwtService.generateAccessToken(user);

        Thread.sleep(1_500);

        assertThatThrownBy(() -> shortLivedJwtService.parseAndValidateAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAndValidateRefreshToken_rejectsExpiredToken() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(AuthTestData.shortLivedJwtProperties());
        String token = shortLivedJwtService.generateRefreshToken(user);

        Thread.sleep(1_500);

        assertThatThrownBy(() -> shortLivedJwtService.parseAndValidateRefreshToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAndValidateAccessToken_rejectsInvalidSignature() {
        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidateAccessToken_rejectsInvalidIssuer() {
        String tokenSignedWithDifferentIssuer = signTokenWithIssuer("wrong-issuer");

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(tokenSignedWithDifferentIssuer))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void getAccessTokenExpirationSeconds_returnsConfiguredValue() {
        assertThat(jwtService.getAccessTokenExpirationSeconds()).isEqualTo(900L);
    }

    private String signTokenWithIssuer(String issuer) {
        JwtProperties properties = AuthTestData.defaultJwtProperties();
        SecretKey signingKey = Keys.hmacShaKeyFor(
                io.jsonwebtoken.io.Decoders.BASE64.decode(properties.secret())
        );
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.accessTokenExpiration())))
                .signWith(signingKey)
                .compact();
    }
}
