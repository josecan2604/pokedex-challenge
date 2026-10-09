package com.arana.pokedex.infrastructure.web.dto;

import com.arana.pokedex.domain.model.LocalPokemon;

import java.time.Instant;
import java.util.List;

public record LocalPokemonResponse(
        int id,
        String name,
        String spriteUrl,
        String category,
        double weightKg,
        double heightM,
        List<String> types,
        List<String> abilities,
        String localizedName,
        String region,
        List<String> tags,
        String notes,
        long version,
        Instant syncedAt,
        Instant updatedAt) {

    public static LocalPokemonResponse from(LocalPokemon pokemon) {
        LocalPokemon.CustomFields custom = pokemon.customFields();
        return new LocalPokemonResponse(
                pokemon.getId(),
                pokemon.getName(),
                pokemon.getSpriteUrl(),
                pokemon.getCategory(),
                pokemon.getWeightKg(),
                pokemon.getHeightM(),
                pokemon.getTypes(),
                pokemon.getAbilities(),
                custom.localizedName(),
                custom.region(),
                custom.tags().stream().sorted().toList(),
                custom.notes(),
                pokemon.getVersion() == null ? 0 : pokemon.getVersion(),
                pokemon.getSyncedAt(),
                pokemon.getUpdatedAt());
    }
}
