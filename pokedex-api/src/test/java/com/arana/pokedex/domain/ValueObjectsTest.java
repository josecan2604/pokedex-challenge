package com.arana.pokedex.domain;

import com.arana.pokedex.domain.exception.ValidationException;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.domain.model.PokemonIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @ParameterizedTest
    @CsvSource({"Pikachu,pikachu", " mr-mime ,mr-mime", "025,25", "1,1"})
    void identifierIsNormalized(String input, String expected) {
        assertThat(PokemonIdentifier.of(input).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "pika chu", "../admin", "pikachu?x=1", "-pikachu"})
    void identifierRejectsUnsafeValues(String input) {
        assertThatThrownBy(() -> PokemonIdentifier.of(input)).isInstanceOf(ValidationException.class);
    }

    @Test
    void identifierRejectsNull() {
        assertThatThrownBy(() -> PokemonIdentifier.of((String) null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void pageRequestComputesOffset() {
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,51"})
    void pageRequestRejectsOutOfRange(int page, int size) {
        assertThatThrownBy(() -> new PageRequest(page, size)).isInstanceOf(ValidationException.class);
    }

    @Test
    void pageResultComputesTotalPagesAndMaps() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2), 0, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.map(i -> i * 10).content()).containsExactly(10, 20);
    }
}
