package com.arana.pokedex.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Makes 401 and 403 responses use the same ProblemDetail body as every other error,
 * while keeping the RFC 6750 {@code WWW-Authenticate} header from Spring's bearer handlers.
 */
public final class ProblemDetailAuthHandlers {

    private ProblemDetailAuthHandlers() {
    }

    public static AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
        BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            delegate.commence(request, response, exception);
            write(response, mapper, HttpStatus.UNAUTHORIZED, "Authentication is required: send a valid Bearer token",
                    request.getRequestURI());
        };
    }

    public static AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        BearerTokenAccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();
        return (request, response, exception) -> {
            delegate.handle(request, response, exception);
            write(response, mapper, HttpStatus.FORBIDDEN, "You do not have permission to perform this action",
                    request.getRequestURI());
        };
    }

    private static void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                              String detail, String path) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(path));
        problem.setProperty("timestamp", Instant.now());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
