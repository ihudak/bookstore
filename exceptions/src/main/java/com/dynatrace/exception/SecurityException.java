package com.dynatrace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class SecurityException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;
        public SecurityException(String message) {
            super(HttpStatus.SERVICE_UNAVAILABLE, message);
        }
}
