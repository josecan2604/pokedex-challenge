package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;

/**
 * User Story 02: the full detail of one Pokemon.
 */
public interface GetPokemonDetailUseCase {

    PokemonDetail getDetail(PokemonIdentifier identifier);
}
