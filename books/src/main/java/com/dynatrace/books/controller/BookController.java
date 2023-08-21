package com.dynatrace.books.controller;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.books.model.Book;
import com.dynatrace.books.repository.BookRepository;
import com.dynatrace.books.repository.ConfigRepository;
import com.dynatrace.controller.HardworkingController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/books")
public class BookController extends HardworkingController {
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ConfigRepository configRepository;
    private final Logger logger = LoggerFactory.getLogger(BookController.class);

    // get all books
    @GetMapping("")
    @Operation(summary = "Get all books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // get a book by id
    @GetMapping("/{id}")
    @Operation(summary = "Get a book by its id")
    public Book getBookById(@PathVariable Long id) {
        simulateHardWork();
        simulateCrash();
        Optional<Book> book = bookRepository.findById(id);
        if (book.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found");
            logger.error(ex.getMessage());
            throw ex;
        }
        return book.get();
    }

    // find a book by isbn
    @GetMapping("/find")
    @Operation(summary = "Get a book by its ISBN13 code")
    public Book getBookByIsbn(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        simulateHardWork();
        simulateCrash();
        logger.info("Looking for book " + isbn);
        Book bookDb = bookRepository.findByIsbn(isbn);
        if (bookDb == null) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book does not exist, ISBN13: " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        return bookDb;
    }

    // ingest a book
    @PostMapping("")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Create a book")
    public Book ingestBook(@RequestBody Book book) {
        simulateHardWork();
        simulateCrash();
        logger.debug("Creating book " + book.getIsbn());
        return bookRepository.save(book);
    }

    // update a book
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update a book by its id")
    public Book updateBookById(@PathVariable Long id, @RequestBody Book book) {
        Optional<Book> bookDB = bookRepository.findById(id);
        if (bookDB.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found");
            logger.error(ex.getMessage());
            throw ex;
        } else if (book.getId() != id || bookDB.get().getId() != id) {
            BadRequestException ex = new BadRequestException("bad book id");
            logger.error(ex.getMessage());
            throw ex;
        }
        return bookRepository.save(book);
    }

    // delete a book
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete a book by its id")
    public void deleteBookById(@PathVariable Long id) {
        bookRepository.deleteById(id);
    }

    // delete all books
    @DeleteMapping("/delete-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete all books")
    public void deleteAllBooks() {
        bookRepository.truncateTable();
    }

    // vend a book by isbn
    @PostMapping("/vend")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Toggle vendible status for an ISBN (vendible => the book can be purchased)")
    public Book vendBookByIsbn(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        simulateHardWork();
        simulateCrash();
        Book book = bookRepository.findByIsbn(isbn);
        if (book == null) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found, ISBN: " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }

        book.setPublished(!book.isPublished());
        return bookRepository.save(book);
    }

    // vend all books
    @PostMapping("/vend-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Make all books vendible")
    public void vendAllBooks() {
        // empty loop to simulate hard work
        simulateHardWork();
        simulateCrash();

//        bookRepository.bulkBookVending(true);
        this.bulkVending(true);
    }

    // unvend all books
    @DeleteMapping("/vend-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Make all books invendible")
    public void unvendAllBooks() {
//        bookRepository.bulkBookVending(false);
        this.bulkVending(false);
    }

    private void bulkVending(boolean vend) {
        for (Book book: bookRepository.findByPublished(!vend)) {
            book.setPublished(vend);
            bookRepository.save(book);
        }
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
