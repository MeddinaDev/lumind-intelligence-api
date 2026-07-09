package com.lumind.api.config;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration bound from {@code application.yml} ({@code jwt.*}).
 * The secret must be a Base64-encoded string with at least 256 bits after decoding
 * (e.g. {@code openssl rand -base64 32}).
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @Positive long accessTokenExpiration,
        @Positive long refreshTokenExpiration
) {

    private static final int MIN_SECRET_BYTES = 32;

    @PostConstruct
    void validateSecretLength() {
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64 string (generate with: openssl rand -base64 32)",
                    ex
            );
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must decode to at least " + MIN_SECRET_BYTES + " bytes (256 bits); got "
                            + decoded.length
            );
        }
    }
}
