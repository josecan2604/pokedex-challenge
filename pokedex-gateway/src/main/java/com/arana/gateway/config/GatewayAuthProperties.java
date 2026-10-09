package com.arana.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * The gateway does not issue tokens; it only validates them. Everything it needs to know
 * about the authorization server is these three values.
 *
 * @param issuer   expected {@code iss} claim
 * @param audience expected {@code aud} claim
 * @param jwksUri  where to fetch the signing keys (pokedex-auth's /oauth2/jwks, or any IdP's)
 * @param rules    access rules, first match wins; unmatched requests require authentication
 */
@ConfigurationProperties(prefix = "gateway.auth")
public record GatewayAuthProperties(String issuer, String audience, String jwksUri, List<AccessRule> rules) {

    public GatewayAuthProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * @param method null matches every method
     * @param path   Spring PathPattern, e.g. {@code /api/local-pokemon/**}
     * @param access PUBLIC, AUTHENTICATED, or a role name such as ADMIN
     */
    public record AccessRule(HttpMethod method, String path, String access) {
    }
}
