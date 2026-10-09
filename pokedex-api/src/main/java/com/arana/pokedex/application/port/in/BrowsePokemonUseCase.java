package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonSummary;

/**
 * User Story 01: browse Pokemon page by page.
 */
public interface BrowsePokemonUseCase {

    PageResult<PokemonSummary> browse(PageRequest pageRequest);
}
