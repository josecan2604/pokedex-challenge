package com.arana.pokedex.domain.exception;

public class ExternalServiceException extends DomainException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
