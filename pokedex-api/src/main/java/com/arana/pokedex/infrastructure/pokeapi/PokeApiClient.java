package com.arana.pokedex.infrastructure.pokeapi;

import com.arana.pokedex.domain.exception.ExternalServiceException;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.EvolutionChainResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.PokemonListResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.PokemonResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.SpeciesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thin HTTP client for PokeAPI. Every call is cached (Caffeine, see application.yml):
 * PokeAPI data is effectively static, and PokeAPI's fair-use policy asks clients to cache.
 * <p>
 * A 404 becomes an empty Optional; any other failure becomes {@link ExternalServiceException}.
 */
@Component
public class PokeApiClient {

    public static final String CACHE_POKEMON_LIST = "pokeapi-pokemon-list";
    public static final String CACHE_POKEMON = "pokeapi-pokemon";
    public static final String CACHE_SPECIES = "pokeapi-species";
    public static final String CACHE_EVOLUTION_CHAIN = "pokeapi-evolution-chain";

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);

    private final RestClient restClient;

    public PokeApiClient(RestClient pokeApiRestClient) {
        this.restClient = pokeApiRestClient;
    }

    @Cacheable(CACHE_POKEMON_LIST)
    public PokemonListResponse getPokemonList(int offset, int limit) {
        return call("pokemon list", () -> restClient.get()
                .uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(PokemonListResponse.class));
    }

    @Cacheable(CACHE_POKEMON)
    public Optional<PokemonResponse> getPokemon(String idOrName) {
        return callOptional("pokemon " + idOrName, () -> restClient.get()
                .uri("/pokemon/{idOrName}", idOrName)
                .retrieve()
                .body(PokemonResponse.class));
    }

    @Cacheable(CACHE_SPECIES)
    public Optional<SpeciesResponse> getSpecies(int speciesId) {
        return callOptional("species " + speciesId, () -> restClient.get()
                .uri("/pokemon-species/{id}", speciesId)
                .retrieve()
                .body(SpeciesResponse.class));
    }

    @Cacheable(CACHE_EVOLUTION_CHAIN)
    public Optional<EvolutionChainResponse> getEvolutionChain(int chainId) {
        return callOptional("evolution chain " + chainId, () -> restClient.get()
                .uri("/evolution-chain/{id}", chainId)
                .retrieve()
                .body(EvolutionChainResponse.class));
    }

    private <T> Optional<T> callOptional(String description, Supplier<T> request) {
        try {
            return Optional.ofNullable(call(description, request));
        } catch (ExternalServiceException e) {
            if (e.getCause() instanceof HttpClientErrorException clientError
                    && clientError.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> T call(String description, Supplier<T> request) {
        try {
            log.debug("PokeAPI request: {}", description);
            return request.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.warn("PokeAPI rejected {}: {}", description, e.getStatusCode());
            }
            throw new ExternalServiceException("PokeAPI rejected the request for " + description, e);
        } catch (RestClientException e) {
            log.error("PokeAPI request failed for {}", description, e);
            throw new ExternalServiceException("PokeAPI is unavailable, please retry later", e);
        }
    }
}
