package com.arana.pokedex.infrastructure.pokeapi;

import com.arana.pokedex.domain.exception.ExternalServiceException;
import com.arana.pokedex.domain.model.EvolutionStage;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PokeApiCatalogAdapterTest {

    private static final String BASE = "https://pokeapi.test/api/v2";

    private MockRestServiceServer server;
    private PokeApiCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        adapter = new PokeApiCatalogAdapter(new PokeApiClient(builder.build()));
    }

    @Test
    void findPageEnrichesEachEntryWithPokemonAndSpeciesData() throws IOException {
        server.expect(requestTo(BASE + "/pokemon?offset=0&limit=1")).andRespond(json("list.json"));
        server.expect(requestTo(BASE + "/pokemon/bulbasaur")).andRespond(json("pokemon-1.json"));
        server.expect(requestTo(BASE + "/pokemon-species/1")).andRespond(json("species-1.json"));

        PageResult<PokemonSummary> page = adapter.findPage(new PageRequest(0, 1));

        assertThat(page.totalElements()).isEqualTo(1302);
        assertThat(page.content()).singleElement().satisfies(bulbasaur -> {
            assertThat(bulbasaur.id()).isEqualTo(1);
            assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
            assertThat(bulbasaur.weightKg()).isEqualTo(6.9);
            assertThat(bulbasaur.types()).containsExactly("grass", "poison");
            assertThat(bulbasaur.abilities()).containsExactly("overgrow", "chlorophyll");
            assertThat(bulbasaur.spriteUrl()).endsWith("/1.png");
        });
        server.verify();
    }

    @Test
    void findDetailMapsStatsDescriptionAndEvolutionChain() throws IOException {
        server.expect(requestTo(BASE + "/pokemon/1")).andRespond(json("pokemon-1.json"));
        server.expect(requestTo(BASE + "/pokemon-species/1")).andRespond(json("species-1.json"));
        server.expect(requestTo(BASE + "/evolution-chain/1")).andRespond(json("evolution-chain-1.json"));

        PokemonDetail detail = adapter.findDetail(PokemonIdentifier.of(1)).orElseThrow();

        assertThat(detail.imageUrl()).contains("official-artwork");
        assertThat(detail.heightM()).isEqualTo(0.7);
        assertThat(detail.description())
                .isEqualTo("While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.");
        assertThat(detail.stats()).extracting(s -> s.name() + "=" + s.baseValue()).containsExactly("hp=45", "attack=49");
        assertThat(detail.evolutionChain()).extracting(EvolutionStage::name, EvolutionStage::stage, EvolutionStage::evolvesFromId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("bulbasaur", 1, null),
                        org.assertj.core.groups.Tuple.tuple("ivysaur", 2, 1),
                        org.assertj.core.groups.Tuple.tuple("venusaur", 3, 2));
    }

    @Test
    void findDetailReturnsEmptyOn404() {
        server.expect(requestTo(BASE + "/pokemon/missingno")).andRespond(withResourceNotFound());

        Optional<PokemonDetail> detail = adapter.findDetail(PokemonIdentifier.of("missingno"));

        assertThat(detail).isEmpty();
    }

    @Test
    void upstreamServerErrorBecomesExternalServiceException() {
        server.expect(requestTo(BASE + "/pokemon/1")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.findDetail(PokemonIdentifier.of(1)))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void upstreamRateLimitBecomesExternalServiceException() {
        server.expect(requestTo(BASE + "/pokemon/1")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> adapter.findDetail(PokemonIdentifier.of(1)))
                .isInstanceOf(ExternalServiceException.class);
    }

    private static org.springframework.test.web.client.ResponseCreator json(String file) throws IOException {
        String body = new ClassPathResource("pokeapi/" + file).getContentAsString(StandardCharsets.UTF_8);
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }
}
