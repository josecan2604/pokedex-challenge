package com.arana.auth.infrastructure.web;

import com.arana.auth.application.port.in.IssueTokenUseCase;
import com.arana.auth.domain.exception.InvalidCredentialsException;
import com.arana.auth.domain.model.AccessToken;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 token endpoint (RFC 6749 section 4.3, Resource Owner Password Credentials grant).
 * <p>
 * Errors follow RFC 6749 section 5.2 rather than ProblemDetail, because OAuth clients expect
 * {@code {"error": "...", "error_description": "..."}}: {@code invalid_request},
 * {@code unsupported_grant_type} or {@code invalid_grant}, all with HTTP 400.
 * <p>
 * The password grant is deprecated in OAuth 2.1. It is used because the SPA is a first-party
 * client with its own login form. The production path is Authorization Code + PKCE (this
 * server, Spring Authorization Server, or an IdP such as Keycloak); the gateway and the API
 * would not change, since they only depend on the JWKS, the issuer and the audience.
 */
@RestController
@Tag(name = "OAuth2", description = "Token endpoint and signing keys")
public class TokenController {

    static final String GRANT_PASSWORD = "password";

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final IssueTokenUseCase issueToken;

    public TokenController(IssueTokenUseCase issueToken) {
        this.issueToken = issueToken;
    }

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exchange a username and password for an access token (grant_type=password)")
    public ResponseEntity<TokenResponse> token(@RequestParam MultiValueMap<String, String> form) {
        String grantType = form.getFirst("grant_type");
        if (!StringUtils.hasText(grantType)) {
            return error("invalid_request", "grant_type is required");
        }
        if (!GRANT_PASSWORD.equals(grantType)) {
            return error("unsupported_grant_type", "Only the password grant is supported");
        }
        String username = form.getFirst("username");
        String password = form.getFirst("password");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return error("invalid_request", "username and password are required");
        }

        try {
            AccessToken token = issueToken.issueForCredentials(username, password);
            log.info("Issued access token for '{}'", username.trim().toLowerCase());
            // RFC 6749 section 5.1: token responses must not be cached.
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(new TokenResponse(token.value(), "Bearer", token.expiresIn().toSeconds(), null, null));
        } catch (InvalidCredentialsException e) {
            log.info("Rejected password grant for '{}'", username.trim().toLowerCase());
            return error("invalid_grant", e.getMessage());
        }
    }

    private static ResponseEntity<TokenResponse> error(String code, String description) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(null, null, null, code, description));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("error") String error,
            @JsonProperty("error_description") String errorDescription) {
    }
}
