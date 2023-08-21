package com.dynatrace.orders.repository;

import com.dynatrace.exception.PurchaseForbiddenException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.model.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class StorageRepository {
    @Value("${http.service.storage}")
    private String storageBaseURL;

    private RestTemplate restTemplate;
    private Logger logger = LoggerFactory.getLogger(StorageRepository.class);

    public StorageRepository() {
        restTemplate = new RestTemplate();
    }



    public Storage buyBook(@NonNull Storage storage) {
        String urlBuilder = storageBaseURL +
                "/sell-book";
        logger.info("Taking from storage");
        logger.info(urlBuilder);
        Storage storageNew = restTemplate.postForObject(urlBuilder, storage, Storage.class);
        if (storageNew == null || storageNew.getQuantity() < 0) {
            PurchaseForbiddenException ex = new PurchaseForbiddenException("Purchase was rejected, ISBN: " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        return storageNew;
    }

    public Storage returnBook(@NonNull Storage storage) {
        String urlBuilder = storageBaseURL +
                "/ingest-book";
        logger.info("Returning to storage");
        logger.info(urlBuilder);
        Storage storageNew = restTemplate.postForObject(urlBuilder, storage, Storage.class);
        if (storageNew == null || storageNew.getQuantity() < 0) {
            PurchaseForbiddenException ex = new PurchaseForbiddenException("Return was rejected, ISBN: " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        return storageNew;
    }

    public Storage getStorageByISBN(String isbn) {
        String urlBuilder = storageBaseURL +
                "/findByISBN" +
                "?isbn=" +
                isbn;
        logger.info("Checking in storage");
        logger.info(urlBuilder);
        Storage storage = restTemplate.getForObject(urlBuilder, Storage.class);
        if (null == storage) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book in Storage is not found by isbn: " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        return storage;
    }

    public Storage[] getAllBooksInStorage() {
        return restTemplate.getForObject(storageBaseURL, Storage[].class);
    }
}
