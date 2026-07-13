package com.lumind.api.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON {@link com.lumind.api.common.exception.ErrorResponse} when a protected resource
 * is accessed without valid authentication in the security context.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MSG_AUTHENTICATION_REQUIRED = "Authentication required";

    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        errorResponseWriter.write(response, request, HttpStatus.UNAUTHORIZED, MSG_AUTHENTICATION_REQUIRED);
    }
}
