package com.dynatrace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class AlreadyPaidException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    public AlreadyPaidException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
