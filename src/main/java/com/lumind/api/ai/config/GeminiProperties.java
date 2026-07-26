package com.lumind.api.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Gemini integration settings bound from {@code application.yml} ({@code lumind.ai.gemini.*}).
 */
@ConfigurationProperties(prefix = "lumind.ai.gemini")
@Validated
public record GeminiProperties(
        String apiKey,
        @NotBlank String model,
        @NotNull Duration timeout,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature
) {
}
