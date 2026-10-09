package com.arana.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire format of the PokeAPI v2 endpoints we use. Only the fields we read are mapped;
 * unknown properties are ignored (Spring Boot's default ObjectMapper setting).
 */
public final class PokeApiDtos {

    private PokeApiDtos() {
    }

    public record NamedResource(String name, String url) {
    }

    /** GET /pokemon?offset=&limit= */
    public record PokemonListResponse(int count, List<NamedResource> results) {
    }

    /** GET /pokemon/{idOrName} */
    public record PokemonResponse(
            int id,
            String name,
            int height,
            int weight,
            NamedResource species,
            Sprites sprites,
            List<TypeSlot> types,
            List<AbilitySlot> abilities,
            List<StatEntry> stats) {
    }

    public record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
    }

    public record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    public record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    public record TypeSlot(int slot, NamedResource type) {
    }

    public record AbilitySlot(NamedResource ability, @JsonProperty("is_hidden") boolean hidden, int slot) {
    }

    public record StatEntry(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
    }

    /** GET /pokemon-species/{id} */
    public record SpeciesResponse(
            int id,
            String name,
            List<Genus> genera,
            @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
            @JsonProperty("evolution_chain") ApiResource evolutionChain) {
    }

    public record Genus(String genus, NamedResource language) {
    }

    public record FlavorText(@JsonProperty("flavor_text") String flavorText, NamedResource language, NamedResource version) {
    }

    public record ApiResource(String url) {
    }

    /** GET /evolution-chain/{id} */
    public record EvolutionChainResponse(int id, ChainLink chain) {
    }

    public record ChainLink(NamedResource species, @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
    }
}
