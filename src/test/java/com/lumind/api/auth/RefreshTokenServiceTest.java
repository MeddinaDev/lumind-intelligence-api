package com.lumind.api.auth;

import com.lumind.api.auth.entity.RefreshToken;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.auth.repository.RefreshTokenRepository;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.common.exception.AccountDisabledException;
import com.lumind.api.common.exception.InvalidRefreshTokenException;
import com.lumind.api.common.util.Sha256Hasher;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private Claims refreshClaims;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        refreshClaims = AuthTestData.refreshClaims(user);
    }

    @Test
    void issueTokens_generatesTokenPairAndPersistsHashedRefreshToken() {
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn(RAW_REFRESH_TOKEN);
        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);

        IssuedTokens issuedTokens = refreshTokenService.issueTokens(user);

        assertThat(issuedTokens.accessToken()).isEqualTo("access-token");
        assertThat(issuedTokens.refreshToken()).isEqualTo(RAW_REFRESH_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken persisted = captor.getValue();
        assertThat(persisted.getUser()).isEqualTo(user);
        assertThat(persisted.getToken()).isEqualTo(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN));
        assertThat(persisted.getExpiresAt()).isEqualTo(refreshClaims.getExpiration().toInstant());
        assertThat(persisted.isRevoked()).isFalse();
    }

    @Test
    void rotate_validRefreshToken_revokesPreviousAndIssuesNewPair() {
        RefreshToken stored = AuthTestData.storedRefreshToken(user, RAW_REFRESH_TOKEN, false);

        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.of(stored));
        when(jwtService.extractUserId(refreshClaims)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.parseAndValidateRefreshToken("new-refresh-token")).thenReturn(AuthTestData.refreshClaims(user));

        IssuedTokens rotated = refreshTokenService.rotate(RAW_REFRESH_TOKEN);

        assertThat(rotated.accessToken()).isEqualTo("new-access-token");
        assertThat(rotated.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void rotate_revokedRefreshToken_throwsInvalidRefreshTokenException() {
        RefreshToken stored = AuthTestData.storedRefreshToken(user, RAW_REFRESH_TOKEN, true);

        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_expiredStoredRefreshToken_throwsInvalidRefreshTokenException() {
        RefreshToken stored = AuthTestData.storedRefreshToken(user, RAW_REFRESH_TOKEN, false);
        stored.setExpiresAt(Instant.now().minusSeconds(60));

        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_missingStoredRefreshToken_throwsInvalidRefreshTokenException() {
        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_invalidJwt_throwsInvalidRefreshTokenException() {
        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN))
                .thenThrow(new JwtException("invalid token"));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_disabledAccount_throwsAccountDisabledException() {
        User disabledUser = AuthTestData.disabledUser();
        Claims disabledUserClaims = AuthTestData.refreshClaims(disabledUser);
        RefreshToken stored = AuthTestData.storedRefreshToken(disabledUser, RAW_REFRESH_TOKEN, false);

        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(disabledUserClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.of(stored));
        when(jwtService.extractUserId(disabledUserClaims)).thenReturn(disabledUser.getId());
        when(userRepository.findById(disabledUser.getId())).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(AccountDisabledException.class);

        verify(refreshTokenRepository, never()).save(stored);
    }

    @Test
    void rotate_missingUser_throwsInvalidRefreshTokenException() {
        RefreshToken stored = AuthTestData.storedRefreshToken(user, RAW_REFRESH_TOKEN, false);

        when(jwtService.parseAndValidateRefreshToken(RAW_REFRESH_TOKEN)).thenReturn(refreshClaims);
        when(refreshTokenRepository.findByToken(Sha256Hasher.hashToHex(RAW_REFRESH_TOKEN)))
                .thenReturn(Optional.of(stored));
        when(jwtService.extractUserId(refreshClaims)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void issueTokens_persistsSha256HashOfRawRefreshToken() {
        String anotherRawToken = "another-raw-refresh-token";

        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn(anotherRawToken);
        when(jwtService.parseAndValidateRefreshToken(anotherRawToken)).thenReturn(refreshClaims);

        refreshTokenService.issueTokens(user);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.argThat(
                entity -> entity.getToken().equals(Sha256Hasher.hashToHex(anotherRawToken))
        ));
    }
}
