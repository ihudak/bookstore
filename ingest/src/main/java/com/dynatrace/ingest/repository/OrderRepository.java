package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class OrderRepository implements IngestRepository {
    @Value("${http.service.orders}")
    private String baseURL;
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(OrderRepository.class);
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    public OrderRepository() {
        restTemplate = new RestTemplate();
    }

    @Override
    public Order[] getAll() {
        try {
            return restTemplate.getForObject(baseURL, Order[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object order) {
        logger.info("Creating Order");
        logger.info(baseURL);
        try {
            Object order1 = order == null ? Order.generate() : order;
            logger.info(order1.toString());
            restTemplate.postForObject(baseURL,  order1, Order.class);
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
        Order order = (Order) object;
        if (order == null) {
            order = Order.getRandomOrder();
        }
        if (order == null) {
            return;
        }
        String urlBuilder = baseURL + (order.isCompleted() ? "/cancel" : "/submit");
        try {
            logger.info("Updating Order");
            logger.info(urlBuilder);
            logger.info(order.toString());
            restTemplate.postForObject(urlBuilder, order, Order.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }
    @Override
    public void clearModel() {
        Order.reset();
    }
}
