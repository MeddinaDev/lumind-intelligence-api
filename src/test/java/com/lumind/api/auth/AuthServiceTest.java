package com.lumind.api.auth;

import com.lumind.api.auth.dto.request.LoginRequest;
import com.lumind.api.auth.dto.request.RefreshTokenRequest;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.dto.response.AuthResponse;
import com.lumind.api.auth.mapper.AuthMapper;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.common.exception.AccountDisabledException;
import com.lumind.api.common.exception.EmailAlreadyExistsException;
import com.lumind.api.common.exception.InvalidCredentialsException;
import com.lumind.api.common.exception.InvalidRefreshTokenException;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private IssuedTokens issuedTokens;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        issuedTokens = AuthTestData.sampleIssuedTokens();
        authResponse = AuthTestData.sampleAuthResponse(user);
    }

    @Test
    void register_newUser_encodesPasswordIssuesTokensAndReturnsResponse() {
        RegisterRequest request = AuthTestData.validRegisterRequest();
        User mappedUser = AuthTestData.activeUser(request.email());
        User savedUser = AuthTestData.activeUser(request.email());

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(authMapper.toUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-encoded-password");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(refreshTokenService.issueTokens(savedUser)).thenReturn(issuedTokens);
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(authMapper.toAuthResponse(issuedTokens, savedUser, 900L)).thenReturn(authResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isEqualTo(authResponse);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("bcrypt-encoded-password");
        verify(refreshTokenService).issueTokens(savedUser);
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest request = AuthTestData.validRegisterRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).issueTokens(any());
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = AuthTestData.validLoginRequest();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(refreshTokenService.issueTokens(user)).thenReturn(issuedTokens);
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(authMapper.toAuthResponse(issuedTokens, user, 900L)).thenReturn(authResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isEqualTo(authResponse);
        verify(refreshTokenService).issueTokens(user);
    }

    @Test
    void login_unknownUser_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("missing@example.com", AuthTestData.RAW_PASSWORD);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        LoginRequest request = AuthTestData.validLoginRequest();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_disabledAccount_throwsAccountDisabledException() {
        LoginRequest request = AuthTestData.validLoginRequest();
        User disabledUser = AuthTestData.disabledUser();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(request.password(), disabledUser.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountDisabledException.class);

        verify(refreshTokenService, never()).issueTokens(any());
    }

    @Test
    void refresh_validRefreshToken_returnsNewAuthResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        IssuedTokens rotatedTokens = new IssuedTokens("new-access", "new-refresh");
        AuthResponse refreshedResponse = new AuthResponse(
                rotatedTokens.accessToken(),
                rotatedTokens.refreshToken(),
                "Bearer",
                900L,
                authResponse.user()
        );
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(refreshTokenService.rotate(request.refreshToken())).thenReturn(rotatedTokens);
        when(jwtService.parseAndValidateAccessToken(rotatedTokens.accessToken())).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(authMapper.toAuthResponse(rotatedTokens, user, 900L)).thenReturn(refreshedResponse);

        AuthResponse response = authService.refresh(request);

        assertThat(response).isEqualTo(refreshedResponse);
        verify(refreshTokenService).rotate(request.refreshToken());
    }

    @Test
    void refresh_expiredOrInvalidToken_throwsInvalidRefreshTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-refresh-token");

        when(refreshTokenService.rotate(request.refreshToken()))
                .thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_revokedToken_throwsInvalidRefreshTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked-refresh-token");

        when(refreshTokenService.rotate(request.refreshToken()))
                .thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_disabledAccount_throwsAccountDisabledException() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(refreshTokenService.rotate(request.refreshToken()))
                .thenThrow(new AccountDisabledException());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void refresh_userMissingAfterRotation_throwsInvalidRefreshTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        UUID missingUserId = UUID.randomUUID();

        when(refreshTokenService.rotate(request.refreshToken())).thenReturn(issuedTokens);
        when(jwtService.parseAndValidateAccessToken(issuedTokens.accessToken())).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn(missingUserId);
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
