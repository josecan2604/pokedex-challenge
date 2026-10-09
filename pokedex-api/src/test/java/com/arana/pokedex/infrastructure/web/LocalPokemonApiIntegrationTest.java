package com.arana.pokedex.infrastructure.web;

import com.arana.pokedex.TestData;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocalPokemonApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PokemonCatalogPort catalog;

    static RequestPostProcessor user() {
        return jwt().jwt(j -> j.subject("ash").claim("roles", java.util.List.of("USER")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin").claim("roles", java.util.List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Nested
    class Read {

        @Test
        void listIsPublicAndPaginated() throws Exception {
            mvc.perform(get("/api/local-pokemon").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.first").value(true));
        }

        @Test
        void listCanSearchByTag() throws Exception {
            mvc.perform(get("/api/local-pokemon").param("search", "mascot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name", hasItem("pikachu")));
        }

        @Test
        void getReturnsSeededPokemon() throws Exception {
            mvc.perform(get("/api/local-pokemon/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("charmander"))
                    .andExpect(jsonPath("$.localizedName").value("Hitokage"));
        }

        @Test
        void getMissingIs404ProblemDetail() throws Exception {
            mvc.perform(get("/api/local-pokemon/9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.instance").value("/api/local-pokemon/9999"));
        }

        @Test
        void nonNumericIdIs400() throws Exception {
            mvc.perform(get("/api/local-pokemon/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.id").exists());
        }

        @Test
        void oversizedPageIs400() throws Exception {
            mvc.perform(get("/api/local-pokemon").param("size", "500"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.size").exists());
        }
    }

    @Nested
    class Update {

        @Test
        void requiresAuthentication() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("WWW-Authenticate"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void updatesCustomFields() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.APPLICATION_JSON).content("""
                            {"localizedName":"Zenigame","region":"Kanto","tags":["Starter","water-type"],"notes":"Updated"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags[0]").value("starter"))
                    .andExpect(jsonPath("$.tags[1]").value("water-type"))
                    .andExpect(jsonPath("$.notes").value("Updated"));
        }

        @Test
        void missingPokemonIs404() throws Exception {
            mvc.perform(put("/api/local-pokemon/9999").with(user()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void malformedJsonIs400() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.APPLICATION_JSON).content("{\"tags\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON"));
        }

        @Test
        void wrongJsonTypeIs400() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.APPLICATION_JSON).content("{\"tags\": \"not-a-list\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void beanValidationErrorsAre400WithFieldDetails() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"region\":\"" + "x".repeat(60) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.region").exists());
        }

        @Test
        void domainRuleViolationsAre400WithFieldDetails() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tags\":[\"has spaces!\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.tags").exists());
        }

        @Test
        void staleVersionIs409() throws Exception {
            mvc.perform(put("/api/local-pokemon/152").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"localizedName\":\"x\",\"version\":42}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void unsupportedMediaTypeIs415() throws Exception {
            mvc.perform(put("/api/local-pokemon/7").with(user()).contentType(MediaType.TEXT_PLAIN).content("hi"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    class Sync {

        @Test
        void createsThenRefreshes() throws Exception {
            when(catalog.findDetail(PokemonIdentifier.of("pokemon-500"))).thenReturn(Optional.of(TestData.detailWithId(500)));

            mvc.perform(post("/api/local-pokemon/sync").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idOrName\":\"pokemon-500\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/local-pokemon/500")))
                    .andExpect(jsonPath("$.version").value(0));

            mvc.perform(post("/api/local-pokemon/sync").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idOrName\":\"pokemon-500\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void unknownPokemonIs404() throws Exception {
            when(catalog.findDetail(any())).thenReturn(Optional.empty());

            mvc.perform(post("/api/local-pokemon/sync").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idOrName\":\"missingno\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void blankIdentifierIs400() throws Exception {
            mvc.perform(post("/api/local-pokemon/sync").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idOrName\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.idOrName").exists());
        }

        @Test
        void rangeTooLargeIs400() throws Exception {
            mvc.perform(post("/api/local-pokemon/sync/range").with(user()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fromId\":1,\"toId\":200}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Delete {

        @Test
        void userRoleIsForbidden() throws Exception {
            mvc.perform(delete("/api/local-pokemon/1").with(user()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void adminDeletesThenGets404() throws Exception {
            when(catalog.findDetail(PokemonIdentifier.of(600))).thenReturn(Optional.of(TestData.detailWithId(600)));
            mvc.perform(post("/api/local-pokemon/sync").with(admin()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idOrName\":\"600\"}"));

            mvc.perform(delete("/api/local-pokemon/600").with(admin())).andExpect(status().isNoContent());
            mvc.perform(delete("/api/local-pokemon/600").with(admin())).andExpect(status().isNotFound());
        }
    }
}
