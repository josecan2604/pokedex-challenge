package com.arana.gateway.security;

import com.arana.gateway.config.GatewayAuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.function.SingletonSupplier;

@Configuration
public class SecurityFilterConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(GatewayAuthProperties properties) {
        AccessRules rules = new AccessRules(properties.rules());
        SingletonSupplier<ReactiveJwtDecoder> decoder = SingletonSupplier.of(() -> jwtDecoder(properties));
        return new JwtAuthenticationFilter(rules, decoder);
    }

    static ReactiveJwtDecoder jwtDecoder(GatewayAuthProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
        decoder.setJwtValidator(validator(properties));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validator(GatewayAuthProperties properties) {
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The token audience is not allowed", null));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(properties.issuer()), audience);
    }
}
