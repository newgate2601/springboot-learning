package com.example.learning.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Gateway Service")
                .description("API Gateway Service")
                .version("1.0.0"));
//        return new OpenAPI()
//                .servers(List.of(new Server().url("http://localhost:8080")))
//                .info(new Info().title("Product Service API").version("1.0.0"));
    }
}
