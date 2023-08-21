package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class CartRepository implements IngestRepository {
    @Value("${http.service.carts}")
    private String baseURL;
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(CartRepository.class);
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    public CartRepository() {
        restTemplate = new RestTemplate();
    }

    @Override
    public Cart[] getAll() {
        try {
            return restTemplate.getForObject(baseURL, Cart[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object cart) {
        try {
            logger.info("Creating Cart");
            logger.info(baseURL);
            Object cart1 = cart == null ? Cart.generate() : cart;
            logger.info(cart1.toString());
            restTemplate.postForObject(baseURL, cart1, Cart.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    @Override
    public void create() {
        this.create(null);
    }

    @Override
    public void update(Object object) {

    }
    @Override
    public void clearModel() {
        Cart.reset();
    }
}
