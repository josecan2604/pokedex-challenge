package com.arana.pokedex.infrastructure.pokeapi;

import com.arana.pokedex.domain.model.EvolutionStage;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.ChainLink;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.NamedResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiMapperTest {

    @Test
    void branchingEvolutionsShareTheSameStage() {
        ChainLink eevee = link(133, "eevee",
                link(134, "vaporeon"),
                link(135, "jolteon"),
                link(136, "flareon"));

        List<EvolutionStage> stages = PokeApiMapper.flattenEvolutionChain(eevee);

        assertThat(stages).hasSize(4);
        assertThat(stages.getFirst()).isEqualTo(new EvolutionStage(133, "eevee", 1, null,
                PokeApiMapper.SPRITE_URL_TEMPLATE.formatted(133)));
        assertThat(stages.subList(1, 4)).allSatisfy(s -> {
            assertThat(s.stage()).isEqualTo(2);
            assertThat(s.evolvesFromId()).isEqualTo(133);
        });
    }

    @Test
    void pokemonWithoutEvolutionsHasSingleStage() {
        assertThat(PokeApiMapper.flattenEvolutionChain(link(128, "tauros"))).hasSize(1);
    }

    @Test
    void idIsParsedFromResourceUrlWithOrWithoutTrailingSlash() {
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/pokemon-species/25/")).isEqualTo(25);
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/evolution-chain/10")).isEqualTo(10);
    }

    private static ChainLink link(int id, String name, ChainLink... next) {
        return new ChainLink(new NamedResource(name, "https://pokeapi.co/api/v2/pokemon-species/" + id + "/"), List.of(next));
    }
}
