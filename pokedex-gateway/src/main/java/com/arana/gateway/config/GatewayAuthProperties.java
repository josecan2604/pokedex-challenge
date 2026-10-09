package com.arana.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.auth")
public record GatewayAuthProperties(String issuer, String audience, String jwksUri, List<AccessRule> rules) {

    public GatewayAuthProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public record AccessRule(HttpMethod method, String path, String access) {
    }
}
