package com.lumind.api.auth.security;

import com.lumind.api.auth.JwtService;
import com.lumind.api.auth.model.AuthenticatedUser;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates Bearer access tokens and populates {@link SecurityContextHolder} with an
 * {@link AuthenticatedUser} principal when the token and user state are valid.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String MSG_INVALID_ACCESS_TOKEN = "Invalid or expired access token";
    static final String MSG_ACCOUNT_DISABLED = "Account is disabled";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> token = extractBearerToken(request);

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authenticateRequest(request, response, token.get())) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            String rawToken
    ) throws IOException {
        try {
            Claims claims = jwtService.parseAndValidateAccessToken(rawToken);
            UUID userId = jwtService.extractUserId(claims);

            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                errorResponseWriter.write(response, request, HttpStatus.UNAUTHORIZED, MSG_INVALID_ACCESS_TOKEN);
                return false;
            }

            if (!user.get().isEnabled()) {
                log.warn("Disabled account access attempt for path: {}", request.getRequestURI());
                errorResponseWriter.write(response, request, HttpStatus.FORBIDDEN, MSG_ACCOUNT_DISABLED);
                return false;
            }

            setSecurityContext(request, user.get());
            return true;

        } catch (JwtException ex) {
            log.debug("Access token validation failed: {}", ex.getMessage());
            errorResponseWriter.write(response, request, HttpStatus.UNAUTHORIZED, MSG_INVALID_ACCESS_TOKEN);
            return false;
        }
    }

    private void setSecurityContext(HttpServletRequest request, User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());

        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorization.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
