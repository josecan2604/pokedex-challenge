package com.arana.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param issuer   value of the {@code iss} claim: the public URL clients reach this server at
 *                 (through the gateway). Resource servers check it.
 * @param audience value of the {@code aud} claim: the API the token is meant for
 * @param tokenTtl access token lifetime
 * @param keys     optional PEM key pair; without it a key pair is generated at startup (dev only)
 */
@ConfigurationProperties(prefix = "auth.token")
public record TokenProperties(String issuer, String audience, Duration tokenTtl, Keys keys) {

    public TokenProperties {
        keys = keys == null ? new Keys(null, null, null) : keys;
    }

    public record Keys(String keyId, String privateKeyLocation, String publicKeyLocation) {
    }
}
