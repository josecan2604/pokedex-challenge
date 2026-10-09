package com.arana.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a correlation id (reusing a well-formed incoming one), forwards it
 * downstream, echoes it in the response and writes one access log line per request.
 * A WebFilter, so it also covers the token and JWKS endpoints, not just routed calls.
 */
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    private static final Logger access = LoggerFactory.getLogger("gateway.access");
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);

        long start = System.nanoTime();
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> access.info("{} {} -> {} in {} ms [{}]",
                        request.getMethod(), request.getPath(),
                        exchange.getResponse().getStatusCode() == null ? "-" : exchange.getResponse().getStatusCode().value(),
                        (System.nanoTime() - start) / 1_000_000, correlationId));
    }
}
