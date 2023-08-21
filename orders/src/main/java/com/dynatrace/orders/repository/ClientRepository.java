package com.dynatrace.orders.repository;

import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.model.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class ClientRepository {
    @Value("${http.service.clients}")
    private String clientBaseURL;
    private Logger logger = LoggerFactory.getLogger(ClientRepository.class);

    public ClientRepository() {
        restTemplate = new RestTemplate();
    }

    private RestTemplate restTemplate;

    public Client getClientByEmail(String email) {
        String urlBuilder = clientBaseURL +
                "/find" +
                "?email=" +
                email;

        logger.info("Checking client");
        logger.info(urlBuilder);

        Client client = restTemplate.getForObject(urlBuilder, Client.class);
        if (null == client) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Client not found by email: " + email);
            logger.error(ex.getMessage());
            throw ex;
        }
        return client;
    }

    public Client[] getAllClients() {
        return restTemplate.getForObject(clientBaseURL, Client[].class);
    }
}
