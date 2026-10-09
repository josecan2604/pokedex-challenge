package com.arana.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.token")
public record TokenProperties(String issuer, String audience, Duration tokenTtl, Keys keys) {

    public TokenProperties {
        keys = keys == null ? new Keys(null, null, null) : keys;
    }

    public record Keys(String keyId, String privateKeyLocation, String publicKeyLocation) {
    }
}
