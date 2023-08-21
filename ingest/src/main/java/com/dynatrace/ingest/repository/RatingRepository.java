package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class RatingRepository implements IngestRepository {
    @Value("${http.service.ratings}")
    private String baseURL;
    private final Logger logger = LoggerFactory.getLogger(RatingRepository.class);
    private final RestTemplate restTemplate;
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    public RatingRepository() {
        restTemplate = new RestTemplate();
    }


    @Override
    public Rating[] getAll() {
        try {
            return restTemplate.getForObject(baseURL, Rating[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object rating) {
        logger.info("Creating rating");
        logger.info(baseURL);
        try {
            Object rating1 = rating == null ? Rating.generate() : rating;
            logger.info(rating1.toString());
            restTemplate.postForObject(baseURL, rating1, Rating.class);
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
        Rating.reset();
    }
}
