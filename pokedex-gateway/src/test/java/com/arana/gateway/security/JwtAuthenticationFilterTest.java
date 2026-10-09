package com.arana.gateway.security;

import com.arana.gateway.config.GatewayAuthProperties;
import com.arana.gateway.config.GatewayAuthProperties.AccessRule;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String ISSUER = "http://gateway.test";
    private static final String AUDIENCE = "pokedex-api";

    private static RSAKey signingKey;
    private static RSAKey attackerKey;
    private static JwtAuthenticationFilter filter;

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    @BeforeAll
    static void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("k1").generate();
        attackerKey = new RSAKeyGenerator(2048).keyID("k1").generate();

        GatewayAuthProperties properties = new GatewayAuthProperties(ISSUER, AUDIENCE, "unused",
                List.of(new AccessRule(HttpMethod.GET, "/api/pokemon/**", "PUBLIC"),
                        new AccessRule(HttpMethod.DELETE, "/api/local-pokemon/**", "ADMIN")));
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(SecurityFilterConfig.validator(properties));
        ReactiveJwtDecoder jwtDecoder = decoder;
        filter = new JwtAuthenticationFilter(new AccessRules(properties.rules()), () -> jwtDecoder);
    }

    @Test
    void publicRoutePassesWithoutToken() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/pokemon"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void protectedRouteWithoutTokenIs401WithChallenge() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.put("/api/local-pokemon/1"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"status\":401");
    }

    @Test
    void validTokenIsForwardedWithIdentityHeaders() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.put("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(signingKey, c -> c)));

        filter.filter(exchange, chain).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtAuthenticationFilter.USER_HEADER)).isEqualTo("ash");
        assertThat(headers.getFirst(JwtAuthenticationFilter.ROLES_HEADER)).isEqualTo("USER");
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
    }

    @Test
    void spoofedIdentityHeadersAreStripped() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/pokemon")
                .header(JwtAuthenticationFilter.USER_HEADER, "admin")
                .header(JwtAuthenticationFilter.ROLES_HEADER, "ADMIN"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().containsKey(JwtAuthenticationFilter.USER_HEADER)).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey(JwtAuthenticationFilter.ROLES_HEADER)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() {
        assertRejected(token(attackerKey, c -> c), "invalid_token");
    }

    @Test
    void expiredTokenIs401() {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        assertRejected(token(signingKey, c -> c.issuedAt(past).notBefore(past).expiresAt(past.plusSeconds(60))),
                "The access token has expired");
    }

    @Test
    void wrongAudienceIs401() {
        assertRejected(token(signingKey, c -> c.audience(List.of("another-api"))), "invalid_token");
    }

    @Test
    void wrongIssuerIs401() {
        assertRejected(token(signingKey, c -> c.issuer("http://evil.test")), "invalid_token");
    }

    @Test
    void garbageTokenIs401() {
        assertRejected("not.a.jwt", "invalid_token");
    }

    @Test
    void validTokenWithoutRoleIs403() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.delete("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(signingKey, c -> c)));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains("insufficient_scope");
    }

    @Test
    void adminTokenCanDelete() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.delete("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(signingKey, c -> c.claim("roles", List.of("ADMIN")))));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
    }

    @Test
    void invalidTokenOnPublicRouteIsStillRejected() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/pokemon")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(attackerKey, c -> c)));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void assertRejected(String token, String expectedChallengeFragment) {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.put("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains(expectedChallengeFragment);
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request);
    }

    private static String token(RSAKey key, UnaryOperator<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("ash")
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plusSeconds(600))
                .claim("roles", List.of("USER"));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),
                customizer.apply(claims).build())).getTokenValue();
    }
}
