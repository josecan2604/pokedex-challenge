package com.arana.auth.infrastructure.security;

import com.arana.auth.application.port.out.TokenSignerPort;
import com.arana.auth.domain.model.AccessToken;
import com.arana.auth.domain.model.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Signs RS256 JWTs. Claims:
 * <pre>
 *   iss   issuer (public URL of this server)       sub   username
 *   aud   ["pokedex-api"]                           uid   user id
 *   iat / nbf / exp                                 roles ["USER"] or ["ADMIN"]
 *   jti   unique token id
 * </pre>
 */
@Component
public class JwtTokenSigner implements TokenSignerPort {

    private final JwtEncoder encoder;
    private final TokenProperties properties;
    private final Clock clock;

    public JwtTokenSigner(JwtEncoder encoder, TokenProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AccessToken sign(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.username())
                .audience(List.of(properties.audience()))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("uid", user.id())
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, properties.tokenTtl());
    }
}
