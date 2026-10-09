package com.arana.gateway.security;

import com.arana.gateway.config.GatewayAuthProperties.AccessRule;
import com.arana.gateway.security.AccessRules.HasRole;
import com.arana.gateway.security.AccessRules.Requirement;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessRulesTest {

    private final AccessRules rules = new AccessRules(List.of(
            new AccessRule(HttpMethod.GET, "/api/local-pokemon/**", "PUBLIC"),
            new AccessRule(HttpMethod.DELETE, "/api/local-pokemon/**", "ADMIN"),
            new AccessRule(null, "/api/local-pokemon/**", "AUTHENTICATED"),
            new AccessRule(HttpMethod.POST, "/api/auth/register", "public")));

    @Test
    void firstMatchingRuleWins() {
        assertThat(rules.requirementFor(HttpMethod.GET, "/api/local-pokemon/25")).isEqualTo(Requirement.PUBLIC);
        assertThat(rules.requirementFor(HttpMethod.DELETE, "/api/local-pokemon/25")).isEqualTo(new HasRole("ADMIN"));
        assertThat(rules.requirementFor(HttpMethod.PUT, "/api/local-pokemon/25")).isEqualTo(Requirement.AUTHENTICATED);
    }

    @Test
    void accessIsCaseInsensitive() {
        assertThat(rules.requirementFor(HttpMethod.POST, "/api/auth/register")).isEqualTo(Requirement.PUBLIC);
    }

    @Test
    void unmatchedRequestsRequireAuthentication() {
        assertThat(rules.requirementFor(HttpMethod.GET, "/api/something-new")).isEqualTo(Requirement.AUTHENTICATED);
        assertThat(rules.requirementFor(HttpMethod.GET, "/api/auth/register")).isEqualTo(Requirement.AUTHENTICATED);
    }

    @Test
    void rolePrefixIsOptional() {
        AccessRules prefixed = new AccessRules(List.of(new AccessRule(null, "/admin/**", "ROLE_ADMIN")));

        assertThat(prefixed.requirementFor(HttpMethod.GET, "/admin/x")).isEqualTo(new HasRole("ADMIN"));
    }
}
