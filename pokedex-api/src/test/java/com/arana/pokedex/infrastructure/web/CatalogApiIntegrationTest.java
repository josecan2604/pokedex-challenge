package com.arana.pokedex.infrastructure.web;

import com.arana.pokedex.TestData;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.domain.exception.ExternalServiceException;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PokemonCatalogPort catalog;

    @Test
    void catalogBrowseIsPublicAndCacheable() throws Exception {
        when(catalog.findPage(new PageRequest(0, 20)))
                .thenReturn(new PageResult<>(List.of(TestData.pikachuDetail().toSummary()), 0, 20, 1302));

        mvc.perform(get("/api/pokemon"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=600, public"))
                .andExpect(jsonPath("$.content[0].category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.totalPages").value(66));
    }

    @Test
    void catalogDetailIncludesEvolutionChain() throws Exception {
        when(catalog.findDetail(PokemonIdentifier.of("pikachu"))).thenReturn(Optional.of(TestData.pikachuDetail()));

        mvc.perform(get("/api/pokemon/Pikachu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionChain.length()").value(3))
                .andExpect(jsonPath("$.stats[0].name").value("hp"));
    }

    @Test
    void catalogUnsafeIdentifierIs400() throws Exception {
        mvc.perform(get("/api/pokemon/pika chu"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idOrName").exists());
    }

    @Test
    void pokeApiOutageIs502() throws Exception {
        when(catalog.findDetail(PokemonIdentifier.of(1)))
                .thenThrow(new ExternalServiceException("PokeAPI is unavailable, please retry later", null));

        mvc.perform(get("/api/pokemon/1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("PokeAPI is unavailable, please retry later"));
    }

    @Test
    void userEndpointsNoLongerExistHere() throws Exception {
        // Users moved to pokedex-auth: these paths are unknown to the resource server.
        mvc.perform(post("/api/auth/register")).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/auth/verify")).andExpect(status().isUnauthorized());
    }
}
