package com.arana.pokedex.application;

import com.arana.pokedex.TestData;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.application.service.PokemonCatalogService;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import com.arana.pokedex.domain.model.PokemonSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonCatalogServiceTest {

    @Mock
    PokemonCatalogPort catalog;

    @InjectMocks
    PokemonCatalogService service;

    @Test
    void browseDelegatesToCatalog() {
        PageRequest request = new PageRequest(0, 1);
        PageResult<PokemonSummary> page = new PageResult<>(List.of(TestData.pikachuDetail().toSummary()), 0, 1, 1302);
        when(catalog.findPage(request)).thenReturn(page);

        assertThat(service.browse(request)).isSameAs(page);
    }

    @Test
    void detailReturnsCatalogData() {
        when(catalog.findDetail(PokemonIdentifier.of("pikachu"))).thenReturn(Optional.of(TestData.pikachuDetail()));

        assertThat(service.getDetail(PokemonIdentifier.of("pikachu")).evolutionChain()).hasSize(3);
    }

    @Test
    void detailThrowsNotFoundForUnknownPokemon() {
        when(catalog.findDetail(PokemonIdentifier.of("missingno"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(PokemonIdentifier.of("missingno")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missingno");
    }
}
