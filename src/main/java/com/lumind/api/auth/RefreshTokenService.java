package com.lumind.api.auth;

import com.lumind.api.auth.entity.RefreshToken;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.auth.repository.RefreshTokenRepository;
import com.lumind.api.common.exception.AccountDisabledException;
import com.lumind.api.common.exception.InvalidRefreshTokenException;
import com.lumind.api.common.util.Sha256Hasher;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages refresh token persistence (SHA-256 hashed) and rotation.
 * <p>
 * {@link #rotate(String)} is transactional: if persisting the new refresh token fails,
 * the revocation of the previous token is rolled back as well.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Generates a new access/refresh pair and persists the refresh token hash.
     */
    @Transactional
    public IssuedTokens issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        persistRefreshToken(user, refreshToken);
        return new IssuedTokens(accessToken, refreshToken);
    }

    /**
     * Validates the presented refresh token, revokes it, and issues a rotated pair.
     * Reuse of an already-revoked token is logged at WARN level (possible token theft).
     */
    @Transactional
    public IssuedTokens rotate(String rawRefreshToken) {
        Claims claims = parseRefreshClaims(rawRefreshToken);

        String tokenHash = Sha256Hasher.hashToHex(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            UUID userId = stored.getUser().getId();
            log.warn("Revoked refresh token reuse detected for user_id={}", userId);
            throw new InvalidRefreshTokenException();
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(jwtService.extractUserId(claims))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    private Claims parseRefreshClaims(String rawRefreshToken) {
        try {
            return jwtService.parseAndValidateRefreshToken(rawRefreshToken);
        } catch (JwtException ex) {
            throw new InvalidRefreshTokenException();
        }
    }

    private void persistRefreshToken(User user, String rawRefreshToken) {
        Claims claims = jwtService.parseAndValidateRefreshToken(rawRefreshToken);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setToken(Sha256Hasher.hashToHex(rawRefreshToken));
        entity.setExpiresAt(claims.getExpiration().toInstant());
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
    }
}
