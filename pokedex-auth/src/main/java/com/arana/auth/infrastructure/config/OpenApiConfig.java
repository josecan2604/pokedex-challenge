package com.arana.auth.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Pokedex Auth",
        version = "1.0",
        description = "Authorization server: user registration, OAuth2 password grant, JWKS. "
                + "Reached through the gateway at /auth/** and /oauth2/**."))
public class OpenApiConfig {
}
