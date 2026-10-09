package com.arana.pokedex.infrastructure.web;

import com.arana.pokedex.application.port.in.BrowsePokemonUseCase;
import com.arana.pokedex.application.port.in.GetPokemonDetailUseCase;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;
import com.arana.pokedex.infrastructure.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.function.Function;

/**
 * Public, read-only views over PokeAPI (User Stories 01 and 02).
 * The catalog records are immutable value objects, so they are serialized as they are.
 */
@RestController
@RequestMapping("/api/pokemon")
@Tag(name = "Pokemon catalog", description = "Browse PokeAPI data (public)")
public class PokemonController {

    private static final CacheControl BROWSER_CACHE = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

    private final BrowsePokemonUseCase browsePokemon;
    private final GetPokemonDetailUseCase getPokemonDetail;

    public PokemonController(BrowsePokemonUseCase browsePokemon, GetPokemonDetailUseCase getPokemonDetail) {
        this.browsePokemon = browsePokemon;
        this.getPokemonDetail = getPokemonDetail;
    }

    @GetMapping
    @Operation(summary = "US01 - Paginated list with sprite, category, weight and abilities")
    public ResponseEntity<PageResponse<PokemonSummary>> browse(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        var result = browsePokemon.browse(new PageRequest(page, size));
        return ResponseEntity.ok().cacheControl(BROWSER_CACHE).body(PageResponse.from(result, Function.identity()));
    }

    @GetMapping("/{idOrName}")
    @Operation(summary = "US02 - Detail with image, stats, description and evolution chain")
    public ResponseEntity<PokemonDetail> detail(@PathVariable String idOrName) {
        PokemonDetail detail = getPokemonDetail.getDetail(PokemonIdentifier.of(idOrName));
        return ResponseEntity.ok().cacheControl(BROWSER_CACHE).body(detail);
    }
}
