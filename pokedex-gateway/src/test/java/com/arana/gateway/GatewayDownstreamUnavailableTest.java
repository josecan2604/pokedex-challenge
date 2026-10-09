package com.arana.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * pokedex-auth and pokedex-api are both down (nothing listens on port 1): the gateway must
 * answer 503 problem+json instead of a generic 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"API_URL=http://localhost:1", "AUTH_URL=http://localhost:1"})
class GatewayDownstreamUnavailableTest {

    @Autowired
    WebTestClient client;

    @Test
    void apiDownIs503ProblemDetail() {
        client.get().uri("/api/pokemon").exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(503);
    }

    @Test
    void loginIs503WhenTheAuthServerIsDown() {
        client.post().uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password").with("username", "ash").with("password", "Pikachu123"))
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void tokensCannotBeValidatedWhileTheAuthServerIsDown() {
        // No JWKS cached yet and the auth server is unreachable: a token cannot be checked.
        client.put().uri("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IngifQ.eyJzdWIiOiJhc2gifQ.c2ln")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}
