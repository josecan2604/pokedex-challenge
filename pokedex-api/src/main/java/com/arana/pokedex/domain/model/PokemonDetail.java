package com.arana.pokedex.domain.model;

import java.util.List;

public record PokemonDetail(
        int id,
        String name,
        String imageUrl,
        String spriteUrl,
        String category,
        String description,
        double heightM,
        double weightKg,
        List<String> types,
        List<String> abilities,
        List<PokemonStat> stats,
        List<EvolutionStage> evolutionChain) {

    public PokemonDetail {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
        stats = List.copyOf(stats);
        evolutionChain = List.copyOf(evolutionChain);
    }

    public PokemonSummary toSummary() {
        return new PokemonSummary(id, name, spriteUrl, category, weightKg, types, abilities);
    }
}
