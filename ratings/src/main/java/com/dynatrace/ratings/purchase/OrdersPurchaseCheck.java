package com.dynatrace.ratings.purchase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// Asks orders' GET /findByEmail whether the e-mail holds a completed order for the ISBN.
@Component
public class OrdersPurchaseCheck implements PurchaseCheck {
    private final RestTemplate restTemplate;
    private final String ordersUrl;

    public OrdersPurchaseCheck(@Qualifier("ordersRestTemplate") RestTemplate restTemplate,
                               @Value("${http.service.orders}") String ordersUrl) {
        this.restTemplate = restTemplate;
        this.ordersUrl = ordersUrl;
    }

    @Override
    public Eligibility check(String email, String isbn) {
        URI uri = UriComponentsBuilder.fromUriString(ordersUrl)
                .path("/findByEmail")
                .queryParam("email", "{email}")
                .encode()
                .buildAndExpand(email)
                .toUri();

        ResponseEntity<OrderLine[]> response;
        try {
            response = restTemplate.getForEntity(uri, OrderLine[].class);
        } catch (HttpClientErrorException.NotFound ex) {
            // orders answers 404 from findByEmail when no client holds the e-mail
            return Eligibility.NOT_A_CUSTOMER;
        } catch (ResourceAccessException ex) {
            // its message names the request URL, which carries the e-mail
            throw new OrdersUnavailableException(ex.getCause());
        } catch (RestClientException ex) {
            throw new OrdersUnavailableException(ex);
        }

        if (response.getStatusCode().value() != 200) {
            throw new OrdersUnavailableException(null);
        }
        OrderLine[] orders = response.getBody();
        if (orders == null) {
            throw new OrdersUnavailableException(null);
        }
        boolean hasOrderForBook = false;
        for (OrderLine order : orders) {
            if (order == null || order.isbn() == null || order.completed() == null) {
                throw new OrdersUnavailableException(null);
            }
        }
        for (OrderLine order : orders) {
            if (order.isbn().equals(isbn)) {
                if (order.completed()) {
                    return Eligibility.ELIGIBLE;
                }
                hasOrderForBook = true;
            }
        }
        return hasOrderForBook ? Eligibility.ORDER_NOT_COMPLETED : Eligibility.NO_ORDER_FOR_BOOK;
    }

    // ratings' own copy of the order fields it reads; every other field is ignored
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OrderLine(String isbn, Boolean completed) {
    }
}
