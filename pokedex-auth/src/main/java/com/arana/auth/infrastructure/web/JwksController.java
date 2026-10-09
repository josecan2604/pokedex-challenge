package com.arana.auth.infrastructure.web;

import com.arana.auth.infrastructure.security.TokenProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes the token verification keys. The gateway and the API download and cache them,
 * then validate every token locally: no call to this server per request.
 */
@RestController
@Tag(name = "OAuth2")
public class JwksController {

    private final Map<String, Object> jwks;
    private final TokenProperties properties;

    public JwksController(RSAKey signingKey, TokenProperties properties) {
        // toPublicJWKSet() strips the private parts: the private key never leaves this process.
        this.jwks = new JWKSet(signingKey).toPublicJWKSet().toJSONObject();
        this.properties = properties;
    }

    @GetMapping(value = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Public signing keys (JWKS)")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(jwks);
    }

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authorization server metadata (RFC 8414)")
    public Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", properties.issuer());
        metadata.put("token_endpoint", properties.issuer() + "/oauth2/token");
        metadata.put("jwks_uri", properties.issuer() + "/oauth2/jwks");
        metadata.put("registration_endpoint", properties.issuer() + "/auth/register");
        metadata.put("grant_types_supported", List.of(TokenController.GRANT_PASSWORD));
        metadata.put("token_endpoint_auth_methods_supported", List.of("none"));
        return metadata;
    }
}
