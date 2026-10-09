package com.arana.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    static final StubBackend backend;

    static {
        try {
            backend = new StubBackend();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_URL", backend::baseUrl);
        registry.add("API_URL", backend::baseUrl);
    }

    @AfterAll
    static void stopStub() {
        backend.close();
    }

    @Autowired
    WebTestClient client;

    @BeforeEach
    void longerTimeout() {
        client = client.mutate().responseTimeout(Duration.ofSeconds(20)).build();
    }

    @Test
    void tokenEndpointIsRoutedToTheAuthServer() throws Exception {
        JsonNode body = tokenRequest("ash", "Pikachu123")
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(body.get("token_type").asText()).isEqualTo("Bearer");
        JsonNode claims = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                Base64.getUrlDecoder().decode(body.get("access_token").asText().split("\\.")[1]));
        assertThat(claims.get("sub").asText()).isEqualTo("ash");
    }

    @Test
    void oauthErrorsFromTheAuthServerPassThrough() {
        tokenRequest("ash", "nope")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("invalid_grant");
    }

    @Test
    void registrationIsPublicAndRouted() {
        client.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"brock\",\"email\":\"b@p.com\",\"password\":\"Onix12345\"}")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void jwksIsRoutedAndPublic() {
        client.get().uri("/oauth2/jwks").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.keys[0].kty").isEqualTo("RSA").jsonPath("$.keys[0].d").doesNotExist();
    }

    @Test
    void tokenEndpointAllowsTheFrontendOrigin() {
        client.post().uri("/oauth2/token")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password").with("username", "ash").with("password", "Pikachu123"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void unknownOriginsAreRejected() {
        client.post().uri("/oauth2/token")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void publicRouteIsForwardedWithoutToken() {
        client.get().uri("/api/pokemon?page=0").exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id")
                .expectBody().jsonPath("$.path").isEqualTo("/api/pokemon");
    }

    @Test
    void protectedRouteWithoutTokenIs401AndNeverReachesTheApi() {
        int before = backend.apiCalls;

        client.put().uri("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(401);

        assertThat(backend.apiCalls).isEqualTo(before);
    }

    @Test
    void validTokenIsValidatedWithTheAuthServersJwksAndForwarded() {
        String token = accessToken("ash", "Pikachu123");

        client.put().uri("/api/local-pokemon/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Name", "admin")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk();

        assertThat(backend.lastForwardedHeaders.get("authorization")).isEqualTo("Bearer " + token);
        assertThat(backend.lastForwardedHeaders.get("x-user-name")).isEqualTo("ash");
        assertThat(backend.lastForwardedHeaders.get("x-user-roles")).isEqualTo("USER");
        assertThat(backend.lastForwardedHeaders).containsKey("x-correlation-id");
    }

    @Test
    void tamperedTokenIs401() {
        String[] parts = accessToken("ash", "Pikachu123").split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new String(Base64.getUrlDecoder().decode(parts[1])).replace("USER", "ADMIN").getBytes());

        client.delete().uri("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parts[0] + "." + forgedPayload + "." + parts[2])
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().value(HttpHeaders.WWW_AUTHENTICATE, v -> assertThat(v).contains("invalid_token"));
    }

    @Test
    void userRoleCannotDelete() {
        client.delete().uri("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("ash", "Pikachu123"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminRoleCanDelete() {
        client.delete().uri("/api/local-pokemon/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("admin", "Admin1234"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void downstreamStatusesPassThrough() {
        client.get().uri("/api/local-pokemon/404").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Local Pokemon '404' was not found");
    }

    @Test
    void unroutedPathIs404() {
        client.get().uri("/internal/anything").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("No route matches this path");
    }

    @Test
    void corsPreflightFromTheFrontendIsAllowed() {
        client.options().uri("/api/local-pokemon/1")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    private WebTestClient.ResponseSpec tokenRequest(String username, String password) {
        return client.post().uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("username", username)
                        .with("password", password))
                .exchange();
    }

    private String accessToken(String username, String password) {
        JsonNode body = tokenRequest(username, password).expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return body.get("access_token").asText();
    }
}
