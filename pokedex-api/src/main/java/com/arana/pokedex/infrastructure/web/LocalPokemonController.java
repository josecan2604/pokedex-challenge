package com.arana.pokedex.infrastructure.web;

import com.arana.pokedex.application.port.in.ManageLocalPokemonUseCase;
import com.arana.pokedex.application.port.in.SyncPokemonUseCase;
import com.arana.pokedex.application.port.in.SyncPokemonUseCase.SyncResult;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.infrastructure.web.dto.LocalPokemonResponse;
import com.arana.pokedex.infrastructure.web.dto.PageResponse;
import com.arana.pokedex.infrastructure.web.dto.SyncRequests.SyncOneRequest;
import com.arana.pokedex.infrastructure.web.dto.SyncRequests.SyncRangeRequest;
import com.arana.pokedex.infrastructure.web.dto.SyncRequests.SyncRangeResponse;
import com.arana.pokedex.infrastructure.web.dto.UpdateLocalPokemonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/local-pokemon")
@Tag(name = "Local Pokemon", description = "Replicated Pokemon with proprietary fields")
public class LocalPokemonController {

    private final ManageLocalPokemonUseCase manageLocalPokemon;
    private final SyncPokemonUseCase syncPokemon;

    public LocalPokemonController(ManageLocalPokemonUseCase manageLocalPokemon, SyncPokemonUseCase syncPokemon) {
        this.manageLocalPokemon = manageLocalPokemon;
        this.syncPokemon = syncPokemon;
    }

    @GetMapping
    @Operation(summary = "List local Pokemon, optionally filtered by name, localized name or tag")
    public PageResponse<LocalPokemonResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String search) {
        return PageResponse.from(manageLocalPokemon.list(new PageRequest(page, size), search), LocalPokemonResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one local Pokemon")
    public LocalPokemonResponse get(@PathVariable int id) {
        return LocalPokemonResponse.from(manageLocalPokemon.get(id));
    }

    @PostMapping("/sync")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "US03 - Replicate one Pokemon: 201 when created, 200 when refreshed")
    public ResponseEntity<LocalPokemonResponse> sync(@Valid @RequestBody SyncOneRequest request) {
        SyncResult result = syncPokemon.sync(PokemonIdentifier.of(request.idOrName()));
        LocalPokemonResponse body = LocalPokemonResponse.from(result.pokemon());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/local-pokemon/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PostMapping("/sync/range")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "US03 - Replicate a range of Pokedex numbers (max 50)")
    public SyncRangeResponse syncRange(@Valid @RequestBody SyncRangeRequest request) {
        List<SyncResult> results = syncPokemon.syncRange(request.fromId(), request.toId());
        int created = (int) results.stream().filter(SyncResult::created).count();
        return new SyncRangeResponse(created, results.size() - created,
                results.stream().map(r -> LocalPokemonResponse.from(r.pokemon())).toList());
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "US04 - Replace the custom fields (404 if missing, 400 if invalid, 409 if stale)")
    public LocalPokemonResponse update(@PathVariable int id, @Valid @RequestBody UpdateLocalPokemonRequest request) {
        return LocalPokemonResponse.from(
                manageLocalPokemon.updateCustomFields(id, request.toCustomFields(), request.version()));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Delete a local Pokemon (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        manageLocalPokemon.delete(id);
        return ResponseEntity.noContent().build();
    }
}
