package com.arana.pokedex.domain.model;

import java.util.List;

/**
 * User Story 01: the data shown for each entry while browsing.
 *
 * @param category the species genus, e.g. "Seed Pokémon"
 * @param weightKg mass in kilograms
 * @param abilities the Pokemon's skills
 */
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
