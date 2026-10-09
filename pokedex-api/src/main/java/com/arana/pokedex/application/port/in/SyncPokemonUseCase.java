package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PokemonIdentifier;

import java.util.List;

/**
 * User Story 03: replicate Pokemon from PokeAPI into the local store.
 * Syncing an existing record refreshes the PokeAPI fields and keeps the custom ones.
 */
public interface SyncPokemonUseCase {

    int MAX_RANGE = 50;

    SyncResult sync(PokemonIdentifier identifier);

    /**
     * Syncs Pokedex numbers {@code fromId..toId}, inclusive, at most {@link #MAX_RANGE} at a time.
     */
    List<SyncResult> syncRange(int fromId, int toId);

    record SyncResult(LocalPokemon pokemon, boolean created) {
    }
}
