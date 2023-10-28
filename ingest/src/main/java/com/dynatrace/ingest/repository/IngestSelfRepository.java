package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class IngestSelfRepository {
    @Value("${http.service.ingest}")
    private String baseURL;
    private final Logger logger = LoggerFactory.getLogger(IngestSelfRepository.class);
    public String getBaseURL() {
        return baseURL;
    }
    private final RestTemplate restTemplate;

    public IngestSelfRepository() {
        this.restTemplate = new RestTemplate();
    }

    public String getStatus() {
        logger.info("Getting Ingest Status");
        try {
            return restTemplate.getForObject(baseURL + "/status", String.class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }
}
