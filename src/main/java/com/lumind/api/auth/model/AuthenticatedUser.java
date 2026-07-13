package com.lumind.api.auth.model;

import java.util.UUID;

/**
 * Authenticated principal stored in {@link org.springframework.security.core.context.SecurityContextHolder}
 * after successful JWT validation.
 */
public record AuthenticatedUser(UUID id, String email) {
}
