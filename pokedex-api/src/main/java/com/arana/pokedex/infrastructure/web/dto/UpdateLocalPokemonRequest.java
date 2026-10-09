package com.arana.pokedex.infrastructure.web.dto;

import com.arana.pokedex.domain.model.LocalPokemon;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT body: replaces all the custom fields. Omitted or null fields are cleared.
 * <p>
 * Bean Validation catches malformed shapes early; the domain enforces the business rules
 * (tag format, normalization) so they hold no matter which adapter calls it.
 *
 * @param version the version the client last read; a mismatch returns 409 Conflict
 */
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
