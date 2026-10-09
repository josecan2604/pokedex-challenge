package com.arana.auth.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full stack: controllers, AuthService, BCrypt, JPA on H2 and the demo users seeded at startup.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthServerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Nested
    class Token {

        @Test
        void passwordGrantReturnsTokenVerifiableWithThePublishedJwks() throws Exception {
            String body = token("ash", "Pikachu123")
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").value(3600))
                    .andReturn().getResponse().getContentAsString();
            String accessToken = mapper.readTree(body).get("access_token").asText();

            // Verify the signature exactly like a resource server would: with the public JWKS only.
            String jwks = mvc.perform(get("/oauth2/jwks")).andReturn().getResponse().getContentAsString();
            RSAPublicKey publicKey = JWKSet.parse(jwks).getKeys().getFirst().toRSAKey().toRSAPublicKey();
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("http://localhost:8080"));
            Jwt jwt = decoder.decode(accessToken);

            assertThat(jwt.getSubject()).isEqualTo("ash");
            assertThat(jwt.getAudience()).containsExactly("pokedex-api");
            assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
            assertThat(jwt.getId()).isNotBlank();
        }

        @Test
        void adminGetsAdminRole() throws Exception {
            String body = token("admin", "Admin1234").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String payload = mapper.readTree(body).get("access_token").asText().split("\\.")[1];
            JsonNode claims = mapper.readTree(java.util.Base64.getUrlDecoder().decode(payload));

            assertThat(claims.get("roles").get(0).asText()).isEqualTo("ADMIN");
        }

        @Test
        void wrongPasswordIsInvalidGrant() throws Exception {
            token("ash", "wrong-password1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_grant"))
                    .andExpect(jsonPath("$.error_description").value("Invalid username or password"));
        }

        @Test
        void unknownUserIsTheSameInvalidGrant() throws Exception {
            token("gary", "Eevee1234")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid username or password"));
        }

        @Test
        void unsupportedGrantType() throws Exception {
            mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "client_credentials"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
        }

        @Test
        void missingParametersAreInvalidRequest() throws Exception {
            mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
            mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "password").param("username", "ash"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        void jsonBodyIsRejected() throws Exception {
            mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        private ResultActions token(String username, String password) throws Exception {
            return mvc.perform(post("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "password")
                    .param("username", username)
                    .param("password", password));
        }
    }

    @Nested
    class Keys {

        @Test
        void jwksPublishesOnlyThePublicKey() throws Exception {
            mvc.perform(get("/oauth2/jwks"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                    .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                    .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                    .andExpect(jsonPath("$.keys[0].kid").exists())
                    .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                    .andExpect(jsonPath("$.keys[0].p").doesNotExist());
        }

        @Test
        void metadataPointsAtTheEndpoints() throws Exception {
            mvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8080/oauth2/token"))
                    .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/oauth2/jwks"));
        }
    }

    @Nested
    class Register {

        @Test
        void createsUserAndTheyCanLogIn() throws Exception {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"Brock\",\"email\":\"brock@pewter.com\",\"password\":\"Onix12345\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("brock"))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());

            mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "password").param("username", "brock").param("password", "Onix12345"))
                    .andExpect(status().isOk());
        }

        @Test
        void duplicateUsernameIs409() throws Exception {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ADMIN\",\"email\":\"other@pokedex.local\",\"password\":\"Admin1234\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(header().string("Content-Type", containsString("application/problem+json")));
        }

        @Test
        void invalidValuesAre400WithFieldErrors() throws Exception {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"x!\",\"email\":\"nope\",\"password\":\"short\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.password").exists());
        }

        @Test
        void missingFieldsAre400() throws Exception {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());
        }

        @Test
        void malformedJsonIs400() throws Exception {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON"));
        }
    }
}
