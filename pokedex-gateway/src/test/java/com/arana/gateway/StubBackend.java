package com.arana.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays both pokedex-auth (token endpoint, JWKS, register) and pokedex-api (/api/**) on the JDK's
 * HTTP server, so gateway tests need no extra dependency. Tokens are real RS256 JWTs signed with
 * a key published on /oauth2/jwks, exactly like the real authorization server.
 * Records the headers of the last /api call so tests can assert what the gateway forwarded.
 */
public final class StubBackend implements AutoCloseable {

    static final String ISSUER = "http://localhost:8080";

    private final HttpServer server;
    private final RSAKey signingKey;
    public final Map<String, String> lastForwardedHeaders = new ConcurrentHashMap<>();
    public volatile int apiCalls;

    public StubBackend() throws IOException, JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("stub-key").generate();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/oauth2/jwks", ex -> respond(ex, 200, new JWKSet(signingKey).toPublicJWKSet().toString()));
        server.createContext("/oauth2/token", this::token);
        server.createContext("/auth/register", ex -> respond(ex, 201, "{\"id\":9,\"username\":\"brock\",\"role\":\"USER\"}"));
        server.createContext("/api/", this::api);
        server.start();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void token(HttpExchange exchange) throws IOException {
        String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String role = form.contains("username=admin") && form.contains("password=Admin1234") ? "ADMIN"
                : form.contains("username=ash") && form.contains("password=Pikachu123") ? "USER"
                : null;
        if (role == null) {
            respond(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid username or password\"}");
            return;
        }
        respond(exchange, 200, "{\"access_token\":\"" + sign(role.equals("ADMIN") ? "admin" : "ash", role)
                + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
    }

    private String sign(String username, String role) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(username)
                    .audience("pokedex-api")
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("roles", List.of(role))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private void api(HttpExchange exchange) throws IOException {
        apiCalls++;
        lastForwardedHeaders.clear();
        exchange.getRequestHeaders().forEach((name, values) -> lastForwardedHeaders.put(name.toLowerCase(), String.join(",", values)));
        if (exchange.getRequestURI().getPath().equals("/api/local-pokemon/404")) {
            respond(exchange, 404, "{\"status\":404,\"detail\":\"Local Pokemon '404' was not found\"}");
            return;
        }
        respond(exchange, 200, "{\"ok\":true,\"path\":\"" + exchange.getRequestURI().getPath() + "\"}");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
