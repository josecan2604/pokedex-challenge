package com.arana.pokedex.application;

import com.arana.pokedex.TestData;
import com.arana.pokedex.application.port.in.SyncPokemonUseCase.SyncResult;
import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.application.service.PokemonSyncService;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.exception.ValidationException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonSyncServiceTest {

    @Mock
    PokemonCatalogPort catalog;

    @Mock
    LocalPokemonRepositoryPort repository;

    PokemonSyncService service;

    @BeforeEach
    void setUp() {
        service = new PokemonSyncService(catalog, repository, Clock.fixed(TestData.NOW, ZoneOffset.UTC));
    }

    @Test
    void createsNewLocalPokemonWhenNotStoredYet() {
        when(catalog.findDetail(PokemonIdentifier.of("pikachu"))).thenReturn(Optional.of(TestData.pikachuDetail()));
        when(repository.findById(25)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = service.sync(PokemonIdentifier.of("pikachu"));

        assertThat(result.created()).isTrue();
        assertThat(result.pokemon().getName()).isEqualTo("pikachu");
        assertThat(result.pokemon().customFields()).isEqualTo(LocalPokemon.CustomFields.EMPTY);
    }

    @Test
    void refreshesExistingPokemonAndKeepsCustomFields() {
        LocalPokemon existing = TestData.localPikachu();
        when(catalog.findDetail(PokemonIdentifier.of(25))).thenReturn(Optional.of(TestData.pikachuDetail()));
        when(repository.findById(25)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = service.sync(PokemonIdentifier.of(25));

        assertThat(result.created()).isFalse();
        assertThat(result.pokemon().getLocalizedName()).isEqualTo("Pikachu");
        assertThat(result.pokemon().getAbilities()).containsExactly("static", "lightning-rod");
    }

    @Test
    void unknownPokemonIsNotFoundAndNothingIsSaved() {
        when(catalog.findDetail(PokemonIdentifier.of("missingno"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sync(PokemonIdentifier.of("missingno")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void syncRangeUpsertsEveryId() {
        when(catalog.findDetail(any())).thenAnswer(inv -> {
            int id = Integer.parseInt(((PokemonIdentifier) inv.getArgument(0)).value());
            return Optional.of(TestData.detailWithId(id));
        });
        when(repository.findById(anyInt())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SyncResult> results = service.syncRange(1, 3);

        assertThat(results).extracting(r -> r.pokemon().getId()).containsExactly(1, 2, 3);
        ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(repository, org.mockito.Mockito.times(3)).save(saved.capture());
    }

    @Test
    void syncRangeWritesNothingIfAnyIdIsUnknown() {
        when(catalog.findDetail(PokemonIdentifier.of(1))).thenReturn(Optional.of(TestData.detailWithId(1)));
        when(catalog.findDetail(PokemonIdentifier.of(2))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncRange(1, 2)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"0,5", "10,9", "1,51"})
    void syncRangeValidatesBounds(int from, int to) {
        assertThatThrownBy(() -> service.syncRange(from, to)).isInstanceOf(ValidationException.class);
        verifyNoInteractions(catalog, repository);
    }
}
