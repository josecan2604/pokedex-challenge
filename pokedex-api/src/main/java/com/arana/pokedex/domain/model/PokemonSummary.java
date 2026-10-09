package com.arana.pokedex.domain.model;

import java.util.List;

public record PokemonSummary(
        int id,
        String name,
        String spriteUrl,
        String category,
        double weightKg,
        List<String> types,
        List<String> abilities) {

    public PokemonSummary {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }
}
