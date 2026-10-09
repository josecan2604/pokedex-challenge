package com.arana.gateway.security;

import com.arana.gateway.security.AccessRules.HasRole;
import com.arana.gateway.security.AccessRules.Public;
import com.arana.gateway.security.AccessRules.Requirement;
import com.arana.gateway.web.ProblemResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;

/**
 * Edge authentication for every routed request.
 * <ol>
 *   <li>Looks up what the route needs ({@link AccessRules}).</li>
 *   <li>Validates the Bearer token <em>locally</em>: signature against the cached JWKS keys,
 *       then issuer, audience, expiry and not-before. No call to the issuer per request.</li>
 *   <li>Checks the role, then forwards to the resource server with the original
 *       {@code Authorization} header plus {@code X-User-Name} / {@code X-User-Roles}.</li>
 * </ol>
 * Responses: 401 for a missing, malformed, expired or wrongly signed token; 403 for a valid
 * token without the role; 503 when the signing keys cannot be fetched. All use RFC 6750
 * {@code WWW-Authenticate} headers and an RFC 9457 problem body.
 */
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_HEADER = "X-User-Name";
    public static final String ROLES_HEADER = "X-User-Roles";
    public static final String JWT_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".jwt";
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessRules accessRules;
    private final Supplier<ReactiveJwtDecoder> decoder;

    /**
     * @param decoder supplied lazily, so the JWKS URI can point at this gateway's own port
     */
    public JwtAuthenticationFilter(AccessRules accessRules, Supplier<ReactiveJwtDecoder> decoder) {
        this.accessRules = accessRules;
        this.decoder = decoder;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = withoutSpoofedIdentity(exchange.getRequest());
        ServerWebExchange sanitized = exchange.mutate().request(request).build();
        Requirement requirement = accessRules.requirementFor(request.getMethod(), request.getPath().value());

        String token = bearerToken(request);
        if (requirement instanceof Public && token == null) {
            return chain.filter(sanitized);
        }
        if (token == null) {
            return unauthorized(sanitized, null, "Authentication is required: send a Bearer token");
        }

        return decoder.get().decode(token)
                .flatMap(jwt -> authorize(sanitized, chain, requirement, jwt))
                .onErrorResume(BadJwtException.class, e -> {
                    log.debug("Rejected token: {}", e.getMessage());
                    return unauthorized(sanitized, "invalid_token", describe(e));
                })
                .onErrorResume(JwtException.class, e -> {
                    log.error("Token validation unavailable: {}", e.getMessage());
                    return ProblemResponses.write(sanitized, HttpStatus.SERVICE_UNAVAILABLE,
                            "Token validation is temporarily unavailable, please retry later");
                });
    }

    private Mono<Void> authorize(ServerWebExchange exchange, GatewayFilterChain chain, Requirement requirement, Jwt jwt) {
        List<String> roles = roles(jwt);
        if (requirement instanceof HasRole(String role) && !roles.contains(role)) {
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE,
                    "Bearer error=\"insufficient_scope\", error_description=\"Requires role " + role + "\"");
            return ProblemResponses.write(exchange, HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
        ServerHttpRequest forwarded = exchange.getRequest().mutate()
                .header(USER_HEADER, jwt.getSubject())
                .header(ROLES_HEADER, String.join(",", roles))
                .build();
        exchange.getAttributes().put(JWT_ATTRIBUTE, jwt);
        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    /**
     * Clients must never be able to set identity headers themselves.
     */
    private static ServerHttpRequest withoutSpoofedIdentity(ServerHttpRequest request) {
        if (!request.getHeaders().containsKey(USER_HEADER) && !request.getHeaders().containsKey(ROLES_HEADER)) {
            return request;
        }
        return request.mutate().headers(h -> {
            h.remove(USER_HEADER);
            h.remove(ROLES_HEADER);
        }).build();
    }

    private static String bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles;
    }

    private static String describe(BadJwtException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("expired")) {
            return "The access token has expired";
        }
        if (message.toLowerCase().contains("signature") || message.contains("Signed JWT rejected")) {
            return "The access token signature is invalid";
        }
        return "The access token is invalid";
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String error, String description) {
        String challenge = error == null
                ? "Bearer"
                : "Bearer error=\"" + error + "\", error_description=\"" + description + "\"";
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, challenge);
        return ProblemResponses.write(exchange, HttpStatus.UNAUTHORIZED, description);
    }
}
