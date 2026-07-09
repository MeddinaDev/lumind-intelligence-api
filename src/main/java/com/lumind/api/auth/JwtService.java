package com.lumind.api.auth;

import com.lumind.api.config.JwtProperties;
import com.lumind.api.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generates and validates JWT access and refresh tokens using HS256.
 * Refresh tokens include a {@code type} claim to prevent use as access tokens.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Builds a signed access token for the given user.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Builds a signed refresh token with a unique {@code jti} for rotation tracking.
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.refreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns the configured access token TTL in seconds (for {@code AuthResponse.expiresIn}).
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration();
    }

    /**
     * Parses and validates an access token. Rejects tokens marked as refresh tokens.
     *
     * @throws JwtException if the token is invalid, expired, or is a refresh token
     */
    public Claims parseAndValidateAccessToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Refresh token cannot be used as access token");
        }
        return claims;
    }

    /**
     * Parses and validates a refresh token. Requires {@code type=refresh}.
     *
     * @throws JwtException if the token is invalid, expired, or not a refresh token
     */
    public Claims parseAndValidateRefreshToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Token is not a refresh token");
        }
        return claims;
    }

    /**
     * Extracts the user UUID from the {@code sub} claim.
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
