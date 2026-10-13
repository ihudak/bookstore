package com.dynatrace.ratings.controller;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.PurchaseForbiddenException;
import com.dynatrace.ratings.purchase.OrdersUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

// Puts the reason of a write or summary refusal into Boot's error shape as "message".
// Every other error keeps Boot's default body, which carries no message.
@RestControllerAdvice
public class RatingsErrorAdvice {
    @ExceptionHandler({BadRequestException.class, PurchaseForbiddenException.class, OrdersUnavailableException.class})
    public ResponseEntity<Map<String, Object>> handle(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status);
        body.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        body.put("message", ex.getReason());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
