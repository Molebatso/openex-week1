package com.openex.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc OpenAPI / Swagger UI configuration.
 * UI available at http://localhost:8080/swagger-ui.html
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("OpenEx 3.0 API")
                .version("1.0.0")
                .description("Simulated cryptocurrency exchange — matching engine, wallets, JWT auth, WebSocket market data.")
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"),
            )
        )
}
