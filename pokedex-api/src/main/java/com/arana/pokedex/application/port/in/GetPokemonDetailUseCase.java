package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;

public interface GetPokemonDetailUseCase {

    PokemonDetail getDetail(PokemonIdentifier identifier);
}
