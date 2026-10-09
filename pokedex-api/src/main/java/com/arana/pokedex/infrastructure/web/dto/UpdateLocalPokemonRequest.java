package com.arana.pokedex.infrastructure.web.dto;

import com.arana.pokedex.domain.model.LocalPokemon;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateLocalPokemonRequest(
        @Size(max = LocalPokemon.MAX_LOCALIZED_NAME_LENGTH) String localizedName,
        @Size(max = LocalPokemon.MAX_REGION_LENGTH) String region,
        @Size(max = LocalPokemon.MAX_TAGS) List<@NotNull String> tags,
        @Size(max = LocalPokemon.MAX_NOTES_LENGTH) String notes,
        @PositiveOrZero Long version) {

    public LocalPokemon.CustomFields toCustomFields() {
        return new LocalPokemon.CustomFields(localizedName, region, tags, notes);
    }
}
