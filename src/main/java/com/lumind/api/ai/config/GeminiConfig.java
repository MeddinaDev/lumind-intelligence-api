package com.lumind.api.ai.config;

import com.lumind.api.ai.client.AiLanguageModelClient;
import com.lumind.api.ai.client.gemini.GeminiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Registers Gemini configuration properties and the {@link AiLanguageModelClient} bean.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    RestClient geminiRestClient(GeminiProperties geminiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(geminiProperties.timeout());
        requestFactory.setReadTimeout(geminiProperties.timeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    AiLanguageModelClient aiLanguageModelClient(GeminiProperties geminiProperties, RestClient geminiRestClient) {
        return new GeminiClient(geminiProperties, geminiRestClient);
    }
}
