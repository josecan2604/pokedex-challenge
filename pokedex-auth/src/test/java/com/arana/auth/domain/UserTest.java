package com.arana.auth.domain;

import com.arana.auth.domain.exception.ValidationException;
import com.arana.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void acceptsValidRegistration() {
        assertThatCode(() -> User.validateRegistration("ash_ketchum", "ash@kanto.com", "Pikachu123"))
                .doesNotThrowAnyException();
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() {
        assertThatThrownBy(() -> User.validateRegistration("a!", "not-an-email", "short"))
                .isInstanceOfSatisfying(ValidationException.class, e ->
                        assertThat(e.getErrors()).containsOnlyKeys("username", "email", "password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"onlyletters", "12345678", "Abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789xx"})
    void rejectsWeakOrTooLongPasswords(String password) {
        assertThatThrownBy(() -> User.validateRegistration("ash", "ash@kanto.com", password))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors()).containsKey("password"));
    }

    @Test
    void rejectsMissingValues() {
        assertThatThrownBy(() -> User.validateRegistration(null, null, null))
                .isInstanceOfSatisfying(ValidationException.class, e ->
                        assertThat(e.getErrors()).containsOnlyKeys("username", "email", "password"));
    }

    @Test
    void normalizesUsernameAndEmail() {
        assertThat(User.normalizeUsername("  Ash ")).isEqualTo("ash");
        assertThat(User.normalizeEmail(" ASH@Kanto.COM ")).isEqualTo("ash@kanto.com");
        assertThat(User.normalizeUsername(null)).isNull();
    }
}
