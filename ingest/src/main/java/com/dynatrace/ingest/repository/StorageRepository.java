package com.dynatrace.ingest.repository;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.ingest.model.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class StorageRepository implements IngestRepository {
    @Value("${http.service.storage}")
    private String baseURL;
    private final RestTemplate restTemplate;
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    private final Logger logger = LoggerFactory.getLogger(StorageRepository.class);
    public StorageRepository() {
        restTemplate = new RestTemplate();
    }



    public Storage buyBook(@NonNull Storage storage) {
        String urlBuilder = baseURL +
                "/sell-book";
        Storage storageNew;
        try {
            storageNew = restTemplate.postForObject(urlBuilder, storage, Storage.class);
        } catch (RestClientException restClientException) {
            logger.error(restClientException.getMessage());
            throw restClientException;
        }
        if (storageNew == null || storageNew.getQuantity() < 0) {
            BadRequestException ex = new BadRequestException("Purchase was rejected, ISBN: " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        return storageNew;
    }

    public Storage returnBook(@NonNull Storage storage) {
        String urlBuilder = baseURL +
                "/ingest-book";

        Storage storageNew;
        try {
            storageNew = restTemplate.postForObject(urlBuilder, storage, Storage.class);
        } catch (RestClientException restClientException) {
            logger.error(restClientException.getMessage());
            throw restClientException;
        }
        if (storageNew == null || storageNew.getQuantity() < 0) {
            BadRequestException ex = new BadRequestException("Return was rejected, ISBN: " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        return storageNew;
    }

    @Override
    public Storage[] getAll() {
        try {
            return restTemplate.getForObject(baseURL, Storage[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object bookInStorage) {
        logger.info("Creating Storage Item");
        logger.info(baseURL);
        try {
            Object book =  bookInStorage == null ? Storage.generate() : bookInStorage;
            logger.info(book.toString());
            restTemplate.postForObject(baseURL, book, Storage.class);
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
        if (null != object) {
            this.buyBook((Storage) object);
        }
    }
    @Override
    public void clearModel() {
        Storage.reset();
    }
}
