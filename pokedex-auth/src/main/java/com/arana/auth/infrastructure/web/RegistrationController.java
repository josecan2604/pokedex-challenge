package com.arana.auth.infrastructure.web;

import com.arana.auth.application.port.in.RegisterUserUseCase;
import com.arana.auth.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Users", description = "Self-registration")
public class RegistrationController {

    private final RegisterUserUseCase registerUser;

    public RegistrationController(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new USER account (400 invalid, 409 username or email taken)")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUser.register(request.username(), request.email(), request.password()));
    }

    /** Shape checks only; the domain enforces the actual rules. */
    public record RegisterRequest(
            @NotBlank @Size(max = 30) String username,
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    /** Never exposes the password hash. */
    public record UserResponse(long id, String username, String email, String role, Instant createdAt) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.username(), user.email(), user.role().name(), user.createdAt());
        }
    }
}
