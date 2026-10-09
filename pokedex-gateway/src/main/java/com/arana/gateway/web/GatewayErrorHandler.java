package com.arana.gateway.web;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        Mapped mapped = map(error);
        if (mapped.status().is5xxServerError()) {
            log.error("{} {} failed with {}: {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(), mapped.status().value(), error.toString());
        }
        return ProblemResponses.write(exchange, mapped.status(), mapped.detail());
    }

    static Mapped map(Throwable error) {
        Throwable root = rootCause(error);
        if (error instanceof NotFoundException) {
            return new Mapped(HttpStatus.SERVICE_UNAVAILABLE, "The requested service is unavailable");
        }
        if (isTimeout(error) || isTimeout(root)) {
            return new Mapped(HttpStatus.GATEWAY_TIMEOUT, "The service took too long to respond");
        }
        if (anyInChain(error, ConnectException.class) || anyInChain(error, UnknownHostException.class)) {
            return new Mapped(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable, please retry later");
        }
        if (error instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            status = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
            String detail = status == HttpStatus.NOT_FOUND ? "No route matches this path" : status.getReasonPhrase();
            return new Mapped(status, detail);
        }
        return new Mapped(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private static boolean isTimeout(Throwable t) {
        if (t instanceof ResponseStatusException rse && rse.getStatusCode().value() == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return true;
        }
        return t instanceof TimeoutException || t instanceof ReadTimeoutException || t instanceof ConnectTimeoutException;
    }

    private static boolean anyInChain(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    record Mapped(HttpStatus status, String detail) {
    }
}
