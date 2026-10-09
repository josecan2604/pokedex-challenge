package com.arana.pokedex.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This service is an OAuth2 resource server. pokedex-auth issues the JWTs (RS256)
 * and publishes its public keys as a JWKS; we fetch and cache those keys and validate
 * every token locally, so a request that bypasses the gateway is still checked.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String ROLES_CLAIM = "roles";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies
                .cors(cors -> cors.disable()) // CORS is handled once, at the gateway
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pokemon/**", "/api/local-pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/local-pokemon/**").hasRole("ADMIN")
                        .requestMatchers("/api/local-pokemon/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(ProblemDetailAuthHandlers.entryPoint(mapper))
                        .accessDeniedHandler(ProblemDetailAuthHandlers.accessDeniedHandler(mapper)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(ProblemDetailAuthHandlers.entryPoint(mapper))
                        .accessDeniedHandler(ProblemDetailAuthHandlers.accessDeniedHandler(mapper)))
                .build();
    }

    /**
     * Fetches pokedex-auth's JWKS lazily (on the first protected request) and caches it,
     * so this service can start before pokedex-auth does.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.jwk-set-uri}") String jwkSetUri,
                          @Value("${security.jwt.issuer}") String issuer,
                          @Value("${security.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token audience is not " + audience, null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    /**
     * Maps the {@code roles} claim (["USER"]) to Spring authorities (ROLE_USER).
     */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
