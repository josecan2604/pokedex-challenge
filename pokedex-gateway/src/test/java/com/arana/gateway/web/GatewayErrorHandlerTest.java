package com.arana.gateway.web;

import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayErrorHandlerTest {

    @Test
    void connectionRefusedIs503() {
        assertThat(status(new RuntimeException(new ConnectException("Connection refused")))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void unknownHostIs503() {
        assertThat(status(new UnknownHostException("pokedex-api"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void noServiceInstanceIs503() {
        assertThat(status(NotFoundException.create(true, "Unable to find instance"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void timeoutsAre504() {
        assertThat(status(new TimeoutException())).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(status(ReadTimeoutException.INSTANCE)).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(status(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT))).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void responseStatusExceptionKeepsItsStatus() {
        assertThat(status(new ResponseStatusException(HttpStatus.NOT_FOUND))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED))).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void anythingElseIs500() {
        assertThat(status(new IllegalStateException("boom"))).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static HttpStatus status(Throwable error) {
        return GatewayErrorHandler.map(error).status();
    }
}
