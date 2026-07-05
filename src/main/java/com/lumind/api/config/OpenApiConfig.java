package com.lumind.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lumindOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lumind Intelligence API")
                        .description("Backend de la plataforma de productividad Lumind")
                        .version("0.0.1"));
    }
}
