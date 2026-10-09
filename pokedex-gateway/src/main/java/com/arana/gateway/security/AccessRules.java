package com.arana.gateway.security;

import com.arana.gateway.config.GatewayAuthProperties.AccessRule;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Locale;

/**
 * Decides what a request needs, from the ordered rules in {@code gateway.auth.rules}.
 * The first matching rule wins; anything unmatched requires authentication (secure by default).
 */
public class AccessRules {

    public sealed interface Requirement {
        Requirement PUBLIC = new Public();
        Requirement AUTHENTICATED = new Authenticated();
    }

    public record Public() implements Requirement {
    }

    public record Authenticated() implements Requirement {
    }

    public record HasRole(String role) implements Requirement {
    }

    private record CompiledRule(HttpMethod method, PathPattern pattern, Requirement requirement) {
    }

    private final List<CompiledRule> rules;

    public AccessRules(List<AccessRule> rules) {
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.rules = rules.stream()
                .map(r -> new CompiledRule(r.method(), parser.parse(r.path()), toRequirement(r.access())))
                .toList();
    }

    public Requirement requirementFor(HttpMethod method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        return rules.stream()
                .filter(r -> r.method() == null || r.method().equals(method))
                .filter(r -> r.pattern().matches(container))
                .map(CompiledRule::requirement)
                .findFirst()
                .orElse(Requirement.AUTHENTICATED);
    }

    private static Requirement toRequirement(String access) {
        String normalized = access == null ? "" : access.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PUBLIC" -> Requirement.PUBLIC;
            case "AUTHENTICATED", "" -> Requirement.AUTHENTICATED;
            default -> new HasRole(normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized);
        };
    }
}
