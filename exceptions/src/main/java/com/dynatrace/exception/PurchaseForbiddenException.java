package com.dynatrace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class PurchaseForbiddenException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    public PurchaseForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
