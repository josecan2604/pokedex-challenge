package com.arana.auth.application;

import com.arana.auth.application.port.out.PasswordHasherPort;
import com.arana.auth.application.port.out.TokenSignerPort;
import com.arana.auth.application.port.out.UserRepositoryPort;
import com.arana.auth.application.service.AuthService;
import com.arana.auth.domain.exception.ConflictException;
import com.arana.auth.domain.exception.InvalidCredentialsException;
import com.arana.auth.domain.exception.ValidationException;
import com.arana.auth.domain.model.AccessToken;
import com.arana.auth.domain.model.Role;
import com.arana.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final User ASH = new User(7L, "ash", "ash@kanto.com", "ASH_HASH", Role.USER, NOW);
    private static final AccessToken TOKEN = new AccessToken("jwt", Duration.ofHours(1));

    @Mock
    UserRepositoryPort users;

    @Mock
    PasswordHasherPort hasher;

    @Mock
    TokenSignerPort signer;

    AuthService service;

    @BeforeEach
    void setUp() {
        when(hasher.hash("timing-attack-mitigation")).thenReturn("DUMMY_HASH");
        service = new AuthService(users, hasher, signer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    class Register {

        @Test
        void normalizesHashesAndAssignsUserRole() {
            when(hasher.hash("Pikachu123")).thenReturn("HASH");
            when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User user = service.register("  Ash ", "ASH@Kanto.com", "Pikachu123");

            assertThat(user).isEqualTo(new User(null, "ash", "ash@kanto.com", "HASH", Role.USER, NOW));
        }

        @Test
        void rejectsTakenUsername() {
            when(users.existsByUsername("ash")).thenReturn(true);

            assertThatThrownBy(() -> service.register("ash", "new@kanto.com", "Pikachu123"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Username");
            verify(users, never()).save(any());
        }

        @Test
        void rejectsTakenEmail() {
            when(users.existsByEmail("ash@kanto.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register("misty", "ash@kanto.com", "Pikachu123"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        void validatesBeforeTouchingStorage() {
            assertThatThrownBy(() -> service.register("a", "bad", "123")).isInstanceOf(ValidationException.class);
            verify(users, never()).existsByUsername(any());
            verify(hasher, never()).hash("123");
        }
    }

    @Nested
    class IssueToken {

        @Test
        void signsTokenForValidCredentials() {
            when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
            when(hasher.matches("Pikachu123", "ASH_HASH")).thenReturn(true);
            when(signer.sign(ASH)).thenReturn(TOKEN);

            assertThat(service.issueForCredentials(" ASH ", "Pikachu123")).isEqualTo(TOKEN);
        }

        @Test
        void wrongPasswordIsRejected() {
            when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
            when(hasher.matches("wrong", "ASH_HASH")).thenReturn(false);

            assertThatThrownBy(() -> service.issueForCredentials("ash", "wrong"))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(signer, never()).sign(any());
        }

        @Test
        void unknownUserGetsSameErrorAndStillRunsOneHashComparison() {
            when(users.findByUsername("gary")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.issueForCredentials("gary", "whatever1"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid username or password");
            verify(hasher).matches(eq("whatever1"), eq("DUMMY_HASH"));
        }

        @Test
        void blankInputIsRejectedWithoutLookup() {
            assertThatThrownBy(() -> service.issueForCredentials(" ", "")).isInstanceOf(InvalidCredentialsException.class);
            assertThatThrownBy(() -> service.issueForCredentials(null, null)).isInstanceOf(InvalidCredentialsException.class);
            verify(users, never()).findByUsername(anyString());
        }
    }
}
