package com.arana.auth.infrastructure.persistence;

import com.arana.auth.domain.exception.ConflictException;
import com.arana.auth.domain.model.Role;
import com.arana.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest {

    @Autowired
    UserPersistenceAdapter adapter;

    @Test
    void savesAndFindsByUsername() {
        User saved = adapter.save(user("brock", "brock@pewter.com"));

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findByUsername("brock")).contains(saved);
        assertThat(adapter.existsByUsername("brock")).isTrue();
        assertThat(adapter.existsByEmail("brock@pewter.com")).isTrue();
        assertThat(adapter.findByUsername("nobody")).isEmpty();
    }

    @Test
    void uniqueConstraintBecomesConflict() {
        adapter.save(user("misty", "misty@cerulean.com"));

        assertThatThrownBy(() -> adapter.save(user("misty", "other@cerulean.com")))
                .isInstanceOf(ConflictException.class);
    }

    private static User user(String username, String email) {
        return new User(null, username, email, "$2a$10$hash", Role.USER, Instant.parse("2026-10-09T12:00:00Z"));
    }
}
