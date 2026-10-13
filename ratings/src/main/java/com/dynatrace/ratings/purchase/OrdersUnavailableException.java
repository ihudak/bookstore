package com.dynatrace.ratings.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrdersUnavailableException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    public OrdersUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", cause);
    }
}
