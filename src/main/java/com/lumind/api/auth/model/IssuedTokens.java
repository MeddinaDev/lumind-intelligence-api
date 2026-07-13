package com.lumind.api.auth.model;

/**
 * Pair of JWT tokens returned after issuance or rotation.
 * Does not expose domain entities to callers outside the auth feature.
 */
public record IssuedTokens(String accessToken, String refreshToken) {
}
