package com.lumind.api.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hashing utility for refresh token persistence.
 * Produces a lowercase hexadecimal digest (64 characters) suitable for {@code refresh_tokens.token}.
 */
public final class Sha256Hasher {

    private static final String ALGORITHM = "SHA-256";

    private Sha256Hasher() {
    }

    /**
     * Computes the SHA-256 hash of the given input and returns it as lowercase hex.
     *
     * @param input the raw value to hash (typically a refresh token JWT string)
     * @return 64-character lowercase hexadecimal digest
     */
    public static String hashToHex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ALGORITHM + " algorithm not available", ex);
        }
    }
}
