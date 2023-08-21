package com.dynatrace.ingest.repository;
import com.dynatrace.ingest.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Repository
public class BookRepository implements IngestRepository {
    @Value("${http.service.books}")
    private String baseURL;
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(BookRepository.class);
    public String getBaseURL() {
        return baseURL;
    }
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    public BookRepository() {
        restTemplate = new RestTemplate();
    }



    @Override
    public Book[] getAll() {
        logger.info("Getting all books");
        try {
            return restTemplate.getForObject(baseURL, Book[].class);
        } catch (RestClientException exception) {
            logger.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void create(@Nullable Object book) {
        logger.info("Creating Book " + book == null ? "rand" : book.toString());
        logger.info(baseURL);
        try {
            restTemplate.postForObject(baseURL, book == null ? Book.generate() : book, Book.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    public void create(boolean vend, double price) {
        logger.info("Creating Books");
        logger.info(baseURL);
        try {
            restTemplate.postForObject(baseURL, Book.generate(vend, price), Book.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    public void create(double price) {
        logger.info("Creating Books");
        logger.info(baseURL);
        try {
            restTemplate.postForObject(baseURL, Book.generate(price), Book.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    public void create(boolean vend) {
        logger.info("Creating Books");
        logger.info(baseURL);
        try {
            restTemplate.postForObject(baseURL, Book.generate(vend), Book.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    @Override
    public void create() {
        logger.info("Creating Books");
        logger.info(baseURL);
        try {
            restTemplate.postForObject(baseURL, Book.generate(), Book.class);
        } catch (Exception exception){
            logger.error(exception.getMessage());
        }
    }

    @Override
    public void update(Object object) {

    }

    @Override
    public void clearModel() {
        Book.reset();
    }
}
