package com.arana.pokedex.application;

import com.arana.pokedex.TestData;
import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.application.service.LocalPokemonService;
import com.arana.pokedex.domain.exception.ConflictException;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.exception.ValidationException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.LocalPokemon.CustomFields;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPokemonServiceTest {

    private static final CustomFields NEW_FIELDS = new CustomFields("Pikachu-JP", "Kanto", List.of("electric-mouse"), null);

    @Mock
    LocalPokemonRepositoryPort repository;

    LocalPokemonService service;

    @BeforeEach
    void setUp() {
        service = new LocalPokemonService(repository, Clock.fixed(TestData.NOW, ZoneOffset.UTC));
    }

    @Test
    void listTreatsBlankSearchAsNoFilter() {
        PageRequest request = new PageRequest(0, 10);
        when(repository.findAll(request, null)).thenReturn(new PageResult<>(List.of(), 0, 10, 0));

        service.list(request, "   ");

        verify(repository).findAll(request, null);
    }

    @Test
    void listTrimsSearch() {
        PageRequest request = new PageRequest(0, 10);
        when(repository.findAll(request, "pika")).thenReturn(new PageResult<>(List.of(), 0, 10, 0));

        service.list(request, "  pika ");

        verify(repository).findAll(request, "pika");
    }

    @Test
    void getThrowsNotFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        when(repository.findById(25)).thenReturn(Optional.of(TestData.localPikachu()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalPokemon updated = service.updateCustomFields(25, NEW_FIELDS, 3L);

        assertThat(updated.getLocalizedName()).isEqualTo("Pikachu-JP");
        assertThat(updated.getTags()).containsExactly("electric-mouse");
        assertThat(updated.getNotes()).isNull();
    }

    @Test
    void updateWithoutVersionSkipsConcurrencyCheck() {
        when(repository.findById(25)).thenReturn(Optional.of(TestData.localPikachu()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.updateCustomFields(25, NEW_FIELDS, null).getRegion()).isEqualTo("Kanto");
    }

    @Test
    void updateRejectsStaleVersion() {
        when(repository.findById(25)).thenReturn(Optional.of(TestData.localPikachu()));

        assertThatThrownBy(() -> service.updateCustomFields(25, NEW_FIELDS, 1L)).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateMissingPokemonIsNotFound() {
        when(repository.findById(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomFields(404, NEW_FIELDS, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateWithInvalidFieldsDoesNotSave() {
        when(repository.findById(25)).thenReturn(Optional.of(TestData.localPikachu()));

        assertThatThrownBy(() -> service.updateCustomFields(25, new CustomFields(null, null, List.of("NOT OK"), null), null))
                .isInstanceOf(ValidationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesExisting() {
        when(repository.existsById(25)).thenReturn(true);

        service.delete(25);

        verify(repository).deleteById(25);
    }

    @Test
    void deleteMissingIsNotFound() {
        when(repository.existsById(25)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(25)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(anyInt());
    }
}
