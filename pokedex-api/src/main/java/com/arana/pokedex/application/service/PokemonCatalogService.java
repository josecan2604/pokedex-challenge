package com.arana.pokedex.application.service;

import com.arana.pokedex.application.port.in.BrowsePokemonUseCase;
import com.arana.pokedex.application.port.in.GetPokemonDetailUseCase;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;

import java.util.Objects;

public class PokemonCatalogService implements BrowsePokemonUseCase, GetPokemonDetailUseCase {

    private final PokemonCatalogPort catalog;

    public PokemonCatalogService(PokemonCatalogPort catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    @Override
    public PageResult<PokemonSummary> browse(PageRequest pageRequest) {
        return catalog.findPage(pageRequest);
    }

    @Override
    public PokemonDetail getDetail(PokemonIdentifier identifier) {
        return catalog.findDetail(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Pokemon", identifier));
    }
}
