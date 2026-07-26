package com.lumind.api.ai.client;

/**
 * Provider-agnostic abstraction for text completion from a large language model.
 * Callers depend on this interface only; concrete integrations (e.g. Gemini) stay encapsulated.
 */
public interface AiLanguageModelClient {

    /**
     * Generates a textual completion for the given prompt.
     *
     * @param prompt non-null prompt text to send to the model
     * @return raw generated text from the model (not parsed into business DTOs)
     */
    String generateCompletion(String prompt);
}
