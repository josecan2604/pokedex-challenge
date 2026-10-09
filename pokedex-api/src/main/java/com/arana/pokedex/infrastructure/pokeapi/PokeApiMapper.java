package com.arana.pokedex.infrastructure.pokeapi;

import com.arana.pokedex.domain.model.EvolutionStage;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonStat;
import com.arana.pokedex.domain.model.PokemonSummary;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.AbilitySlot;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.ChainLink;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.EvolutionChainResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.FlavorText;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.PokemonResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.SpeciesResponse;
import com.arana.pokedex.infrastructure.pokeapi.dto.PokeApiDtos.TypeSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class PokeApiMapper {

    static final String SPRITE_URL_TEMPLATE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";
    private static final String ENGLISH = "en";

    private PokeApiMapper() {
    }

    static PokemonSummary toSummary(PokemonResponse pokemon, SpeciesResponse species) {
        return new PokemonSummary(
                pokemon.id(),
                pokemon.name(),
                spriteOf(pokemon),
                categoryOf(species),
                pokemon.weight() / 10.0,
                typesOf(pokemon),
                abilitiesOf(pokemon));
    }

    static PokemonDetail toDetail(PokemonResponse pokemon, SpeciesResponse species, EvolutionChainResponse chain) {
        String sprite = spriteOf(pokemon);
        return new PokemonDetail(
                pokemon.id(),
                pokemon.name(),
                imageOf(pokemon).orElse(sprite),
                sprite,
                categoryOf(species),
                descriptionOf(species),
                pokemon.height() / 10.0,
                pokemon.weight() / 10.0,
                typesOf(pokemon),
                abilitiesOf(pokemon),
                nullSafe(pokemon.stats()).stream()
                        .map(s -> new PokemonStat(s.stat().name(), s.baseStat()))
                        .toList(),
                chain == null ? List.of() : flattenEvolutionChain(chain.chain()));
    }

    static List<EvolutionStage> flattenEvolutionChain(ChainLink root) {
        List<EvolutionStage> stages = new ArrayList<>();
        if (root != null) {
            collect(root, 1, null, stages);
        }
        stages.sort(Comparator.comparingInt(EvolutionStage::stage).thenComparingInt(EvolutionStage::id));
        return stages;
    }

    private static void collect(ChainLink link, int stage, Integer parentId, List<EvolutionStage> out) {
        int id = idFromUrl(link.species().url());
        out.add(new EvolutionStage(id, link.species().name(), stage, parentId, SPRITE_URL_TEMPLATE.formatted(id)));
        for (ChainLink next : nullSafe(link.evolvesTo())) {
            collect(next, stage + 1, id, out);
        }
    }

    static int idFromUrl(String url) {
        Objects.requireNonNull(url, "resource url");
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(trimmed.substring(trimmed.lastIndexOf('/') + 1));
    }

    private static String spriteOf(PokemonResponse pokemon) {
        if (pokemon.sprites() != null && pokemon.sprites().frontDefault() != null) {
            return pokemon.sprites().frontDefault();
        }
        return SPRITE_URL_TEMPLATE.formatted(pokemon.id());
    }

    private static Optional<String> imageOf(PokemonResponse pokemon) {
        return Optional.ofNullable(pokemon.sprites())
                .map(s -> s.other())
                .map(o -> o.officialArtwork())
                .map(a -> a.frontDefault());
    }

    private static String categoryOf(SpeciesResponse species) {
        if (species == null) {
            return null;
        }
        return nullSafe(species.genera()).stream()
                .filter(g -> ENGLISH.equals(g.language().name()))
                .map(g -> g.genus())
                .findFirst()
                .orElse(null);
    }

    private static String descriptionOf(SpeciesResponse species) {
        if (species == null) {
            return null;
        }
        List<FlavorText> english = nullSafe(species.flavorTextEntries()).stream()
                .filter(f -> ENGLISH.equals(f.language().name()))
                .toList();
        if (english.isEmpty()) {
            return null;
        }
        return english.getLast().flavorText()
                .replaceAll("[\\f\\n\\r\\u00ad]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static List<String> typesOf(PokemonResponse pokemon) {
        return nullSafe(pokemon.types()).stream()
                .sorted(Comparator.comparingInt(TypeSlot::slot))
                .map(t -> t.type().name())
                .toList();
    }

    private static List<String> abilitiesOf(PokemonResponse pokemon) {
        return nullSafe(pokemon.abilities()).stream()
                .sorted(Comparator.comparingInt(AbilitySlot::slot))
                .map(a -> a.ability().name())
                .toList();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
