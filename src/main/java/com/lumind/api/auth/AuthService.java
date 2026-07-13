package com.lumind.api.auth;

import com.lumind.api.auth.dto.request.LoginRequest;
import com.lumind.api.auth.dto.request.RefreshTokenRequest;
import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.dto.response.AuthResponse;
import com.lumind.api.auth.mapper.AuthMapper;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.common.exception.AccountDisabledException;
import com.lumind.api.common.exception.EmailAlreadyExistsException;
import com.lumind.api.common.exception.InvalidCredentialsException;
import com.lumind.api.common.exception.InvalidRefreshTokenException;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            AuthMapper authMapper,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authMapper = authMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        AuthResponse response = buildAuthResponse(user);
        log.info("User registered successfully: userId={}", user.getId());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Invalid credentials attempt");
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Invalid credentials attempt");
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        AuthResponse response = buildAuthResponse(user);
        log.info("User logged in successfully: userId={}", user.getId());
        return response;
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        IssuedTokens issuedTokens = refreshTokenService.rotate(request.refreshToken());
        User user = resolveUserFromAccessToken(issuedTokens.accessToken());
        AuthResponse response = buildAuthResponse(user, issuedTokens);
        log.info("Tokens refreshed successfully: userId={}", user.getId());
        return response;
    }

    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponse(user, refreshTokenService.issueTokens(user));
    }

    private AuthResponse buildAuthResponse(User user, IssuedTokens issuedTokens) {
        long expiresIn = jwtService.getAccessTokenExpirationSeconds();
        return authMapper.toAuthResponse(issuedTokens, user, expiresIn);
    }

    private User resolveUserFromAccessToken(String accessToken) {
        Claims claims = jwtService.parseAndValidateAccessToken(accessToken);
        UUID userId = jwtService.extractUserId(claims);
        return userRepository.findById(userId)
                .orElseThrow(InvalidRefreshTokenException::new);
    }
}
