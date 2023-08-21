package com.dynatrace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
public class ServiceBusyException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;
    public ServiceBusyException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
