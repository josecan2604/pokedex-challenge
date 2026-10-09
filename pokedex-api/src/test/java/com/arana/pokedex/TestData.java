package com.arana.pokedex;

import com.arana.pokedex.domain.model.EvolutionStage;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonStat;

import java.time.Instant;
import java.util.List;

public final class TestData {

    public static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private TestData() {
    }

    public static PokemonDetail pikachuDetail() {
        return new PokemonDetail(25, "pikachu",
                "https://img/official/25.png", "https://img/25.png",
                "Mouse Pokémon", "When several of these Pokémon gather, their electricity can build.",
                0.4, 6.0,
                List.of("electric"), List.of("static", "lightning-rod"),
                List.of(new PokemonStat("hp", 35), new PokemonStat("speed", 90)),
                List.of(new EvolutionStage(172, "pichu", 1, null, "https://img/172.png"),
                        new EvolutionStage(25, "pikachu", 2, 172, "https://img/25.png"),
                        new EvolutionStage(26, "raichu", 3, 25, "https://img/26.png")));
    }

    public static PokemonDetail detailWithId(int id) {
        PokemonDetail p = pikachuDetail();
        return new PokemonDetail(id, "pokemon-" + id, p.imageUrl(), p.spriteUrl(), p.category(), p.description(),
                p.heightM(), p.weightKg(), p.types(), p.abilities(), p.stats(), p.evolutionChain());
    }

    public static LocalPokemon localPikachu() {
        return LocalPokemon.restore(25, "pikachu", "https://img/25.png", "Mouse Pokémon", 6.0, 0.4,
                List.of("electric"), List.of("static"),
                new LocalPokemon.CustomFields("Pikachu", "Kanto", List.of("mascot"), "notes"),
                3L, NOW, NOW);
    }
}
