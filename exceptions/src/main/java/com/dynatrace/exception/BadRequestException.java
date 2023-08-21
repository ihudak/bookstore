package com.dynatrace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
