package com.arana.auth.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class SigningKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyConfig.class);

    @Bean
    RSAKey signingKey(TokenProperties properties, ResourceLoader resourceLoader) throws IOException {
        TokenProperties.Keys keys = properties.keys();
        KeyPair pair;
        if (StringUtils.hasText(keys.privateKeyLocation()) && StringUtils.hasText(keys.publicKeyLocation())) {
            pair = new KeyPair(
                    read(resourceLoader, keys.publicKeyLocation(), RsaKeyConverters.x509()),
                    read(resourceLoader, keys.privateKeyLocation(), RsaKeyConverters.pkcs8()));
            log.info("Loaded token signing key from {}", keys.privateKeyLocation());
        } else {
            pair = generate();
            log.warn("No signing key configured (auth.token.keys.*): generated an ephemeral RSA key pair. "
                    + "Tokens become invalid after a restart. Configure PEM keys outside local development.");
        }
        String keyId = StringUtils.hasText(keys.keyId()) ? keys.keyId() : UUID.randomUUID().toString();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey signingKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(signingKey)));
    }

    private static <K> K read(ResourceLoader loader, String location, Converter<InputStream, K> converter) throws IOException {
        try (InputStream in = loader.getResource(location).getInputStream()) {
            return converter.convert(in);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is not available", e);
        }
    }
}
