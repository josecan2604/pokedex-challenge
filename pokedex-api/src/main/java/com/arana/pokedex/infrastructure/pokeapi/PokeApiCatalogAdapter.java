package com.arana.pokedex.infrastructure.pokeapi;

import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.domain.exception.DomainException;
import com.arana.pokedex.domain.exception.ExternalServiceException;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.EvolutionChainResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.NamedResource;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.PokemonListResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.PokemonResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.SpeciesResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class PokeApiCatalogAdapter implements PokemonCatalogPort {

    private final PokeApiClient client;

    public PokeApiCatalogAdapter(PokeApiClient client) {
        this.client = client;
    }

    @Override
    public PageResult<PokemonSummary> findPage(PageRequest pageRequest) {
        PokemonListResponse list = client.getPokemonList(pageRequest.offset(), pageRequest.size());
        List<NamedResource> results = list.results() == null ? List.of() : list.results();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Optional<PokemonSummary>>> futures = results.stream()
                    .map(entry -> executor.submit(() -> loadSummary(entry.name())))
                    .toList();
            List<PokemonSummary> summaries = futures.stream()
                    .map(PokeApiCatalogAdapter::await)
                    .flatMap(Optional::stream)
                    .toList();
            return new PageResult<>(summaries, pageRequest.page(), pageRequest.size(), list.count());
        }
    }

    @Override
    public Optional<PokemonDetail> findDetail(PokemonIdentifier identifier) {
        return client.getPokemon(identifier.value()).map(pokemon -> {
            SpeciesResponse species = speciesOf(pokemon).orElse(null);
            EvolutionChainResponse chain = species == null || species.evolutionChain() == null
                    ? null
                    : client.getEvolutionChain(PokeApiMapper.idFromUrl(species.evolutionChain().url())).orElse(null);
            return PokeApiMapper.toDetail(pokemon, species, chain);
        });
    }

    private Optional<PokemonSummary> loadSummary(String name) {
        return client.getPokemon(name)
                .map(pokemon -> PokeApiMapper.toSummary(pokemon, speciesOf(pokemon).orElse(null)));
    }

    private Optional<SpeciesResponse> speciesOf(PokemonResponse pokemon) {
        if (pokemon.species() == null) {
            return Optional.empty();
        }
        return client.getSpecies(PokeApiMapper.idFromUrl(pokemon.species().url()));
    }

    private static <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Interrupted while calling PokeAPI", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DomainException domainException) {
                throw domainException;
            }
            throw new ExternalServiceException("Unexpected error while calling PokeAPI", e.getCause());
        }
    }
}
