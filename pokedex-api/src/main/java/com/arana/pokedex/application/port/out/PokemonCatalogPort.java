package com.arana.pokedex.application.port.out;

import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;

import java.util.Optional;

public interface PokemonCatalogPort {

    PageResult<PokemonSummary> findPage(PageRequest pageRequest);

    Optional<PokemonDetail> findDetail(PokemonIdentifier identifier);
}
