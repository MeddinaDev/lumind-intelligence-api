package com.lumind.api.ai.client.gemini;

import com.lumind.api.ai.client.AiLanguageModelClient;
import com.lumind.api.ai.config.GeminiProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/**
 * Gemini implementation of {@link AiLanguageModelClient}.
 * Encapsulates all HTTP communication with the Gemini API.
 */
public class GeminiClient implements AiLanguageModelClient {

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com";

    private final GeminiProperties geminiProperties;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties geminiProperties, RestClient restClient) {
        this.geminiProperties = geminiProperties;
        this.restClient = restClient;
    }

    @Override
    public String generateCompletion(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank");
        }

        return executeGenerateContentRequest(prompt);
    }

    /**
     * Sends a generate-content request to Gemini and returns the raw model text.
     * Temporary stub implementation; replace with real HTTP call in a subsequent phase.
     */
    private String executeGenerateContentRequest(String prompt) {
        // Prepared structure for real integration:
        // POST {GEMINI_API_BASE_URL}/v1beta/models/{model}:generateContent?key={apiKey}
        // Body: { "contents": [{ "parts": [{ "text": prompt }] }],
        //        "generationConfig": { "temperature": geminiProperties.temperature() } }
        // Response parsing: extract candidates[0].content.parts[0].text (raw string only)
        Objects.requireNonNull(geminiProperties);
        Objects.requireNonNull(restClient);

        return stubCompletion(prompt);
    }

    private String stubCompletion(String prompt) {
        return """
                {
                  "summary": "Stub analysis generated for development. Prompt length: %d characters.",
                  "insights": [
                    "This is a temporary stub response from GeminiClient.",
                    "Replace executeGenerateContentRequest with the real Gemini API call in the next phase."
                  ],
                  "recommendations": [
                    "Configure GEMINI_API_KEY for production use.",
                    "Implement the HTTP request using the injected RestClient and GeminiProperties."
                  ]
                }
                """.formatted(prompt.length());
    }
}
