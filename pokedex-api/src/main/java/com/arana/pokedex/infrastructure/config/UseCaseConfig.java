package com.arana.pokedex.infrastructure.config;

import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.application.service.LocalPokemonService;
import com.arana.pokedex.application.service.PokemonCatalogService;
import com.arana.pokedex.application.service.PokemonSyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composition root: the application services are plain Java with no Spring annotations,
 * so they are wired here. That keeps the domain and application layers framework-free
 * and trivially unit-testable.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PokemonCatalogService pokemonCatalogService(PokemonCatalogPort catalog) {
        return new PokemonCatalogService(catalog);
    }

    @Bean
    PokemonSyncService pokemonSyncService(PokemonCatalogPort catalog, LocalPokemonRepositoryPort repository, Clock clock) {
        return new PokemonSyncService(catalog, repository, clock);
    }

    @Bean
    LocalPokemonService localPokemonService(LocalPokemonRepositoryPort repository, Clock clock) {
        return new LocalPokemonService(repository, clock);
    }
}
