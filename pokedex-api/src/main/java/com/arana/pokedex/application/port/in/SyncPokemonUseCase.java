package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PokemonIdentifier;

import java.util.List;

public interface SyncPokemonUseCase {

    int MAX_RANGE = 50;

    SyncResult sync(PokemonIdentifier identifier);

    List<SyncResult> syncRange(int fromId, int toId);

    record SyncResult(LocalPokemon pokemon, boolean created) {
    }
}
