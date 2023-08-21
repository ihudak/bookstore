package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class ClientRepository implements IngestRepository {
    @Value("${http.service.clients}")
    private String baseURL;
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(ClientRepository.class);
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    public ClientRepository() {
        restTemplate = new RestTemplate();
    }



    @Override
    public Client[] getAll() {
        try {
            return restTemplate.getForObject(baseURL, Client[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object client) {
        logger.info("Creating Clients");
        logger.info(baseURL);
        Object client0 = client == null ? Client.generate() : client;
        logger.info(client0.toString());
        try {
            Object client1 = restTemplate.postForObject(baseURL, client0, Client.class);
            logger.info("Resulting Client " + client1.toString());
        } catch (Exception exception) {
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
        Client.reset();
    }
}
