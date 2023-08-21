package com.dynatrace.storage.controller;

import com.dynatrace.controller.HardworkingController;
import com.dynatrace.exception.InsufficientResourcesException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.model.Book;
import com.dynatrace.storage.model.Storage;
import com.dynatrace.storage.repository.BookRepository;
import com.dynatrace.storage.repository.ConfigRepository;
import com.dynatrace.storage.repository.StorageRepository;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController extends HardworkingController {
    @Autowired
    private StorageRepository storageRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(StorageController.class);

    // get all storage items
    @GetMapping("")
    public List<Storage> getAllStorageItems() {
        return storageRepository.findAll();
    }

    // get a storage item by id
    @GetMapping("/{id}")
    public Storage getStorageItemById(@PathVariable Long id) {
        Optional<Storage> storage = storageRepository.findById(id);
        if (storage.isEmpty()) {
            logger.error("Book is not found. ID = " + id);
            ResourceNotFoundException ex = new ResourceNotFoundException("Book is not found in the storage");
            logger.error(ex.getMessage());
            throw ex;
        }
        return storage.get();
    }

    // get all users who have the book in their cart
    @GetMapping("/findByISBN")
    public Storage getStorageItemByISBN(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        this.verifyBook(isbn);
        return storageRepository.findByIsbn(isbn);
    }

    // create a book in storage
    @PostMapping("")
    public Storage createStorage(@RequestBody Storage storage) {
        simulateHardWork();
        simulateCrash();
        return ingestBook(storage);
    }

    // add book to storage
    @PostMapping("/ingest-book")
    public Storage addBooksToStorage(@RequestBody Storage storage) {
        simulateHardWork();
        simulateCrash();
        logger.debug("Ingesting book " + storage.getIsbn() + " qty: " + storage.getQuantity());
        return ingestBook(storage);
    }

    private Storage ingestBook(Storage storage) {
        simulateHardWork();
        simulateCrash();

        this.verifyBook(storage.getIsbn());

        Storage storageDb = storageRepository.findByIsbn(storage.getIsbn());
        if (null == storageDb) {
            return storageRepository.save(storage);
        }
        storageDb.setQuantity(storageDb.getQuantity() + storage.getQuantity());
        logger.debug("Ingested book " + storage.getIsbn());
        return storageRepository.save(storageDb);
    }

    @PostMapping("/sell-book")
    public Storage sellBooksFromStorage(@RequestBody Storage storage) {
        simulateHardWork();
        simulateCrash();
        this.verifyBook(storage.getIsbn());
        Book book = bookRepository.getBookByISBN(storage.getIsbn());
        Storage storageDb = storageRepository.findByIsbn(storage.getIsbn());
        if (book == null) {
            logger.error("Trying to sell a book that is not registered " + storage.getIsbn());
        } else if (storageDb == null) {
            logger.error("Trying to sell a book that is not in the inventory " + storage.getIsbn());
        } else {
            logger.info("Selling book " + storage.getIsbn() + " qty: " + storage.getQuantity() + " while inventory has " + storageDb.getQuantity());
        }
        if (null == book || storageDb == null || storageDb.getQuantity() < storage.getQuantity()) {
            InsufficientResourcesException ex = new InsufficientResourcesException("Insufficient books in the storage, ISBN " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        try {
            storageDb.setQuantity(storageDb.getQuantity() - storage.getQuantity());
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            throw new InsufficientResourcesException(exception.getMessage());
        }
        logger.debug("Sold book " + storage.getIsbn());
        return storageRepository.save(storageDb);
    }

    // update a storage item
    @PutMapping("/{id}")
    public Storage updateStorageById(@PathVariable Long id, @RequestBody Storage storage) {
        simulateHardWork();
        simulateCrash();
        this.verifyBook(storage.getIsbn());

        Optional<Storage> storageDb = storageRepository.findById(id);
        if (storageDb.isEmpty() || storageDb.get().getId() != storage.getId()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book is not found, ISBN: " + storage.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        return storageRepository.save(storage);
    }

    // delete an item from storage
    @DeleteMapping("/{id}")
    public void deleteStorageItemById(@PathVariable Long id) {
        storageRepository.deleteById(id);
    }

    // delete all books
    @DeleteMapping("/delete-all")
    public void deleteAllBooksFromStorage() {
        storageRepository.truncateTable();
    }

    private void verifyBook(String isbn) {
        Book book = bookRepository.getBookByISBN(isbn);
        if (null == book) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found by isbn " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        logger.debug(book.getIsbn());
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
