package com.dynatrace.ratings.controller;

import com.dynatrace.controller.HardworkingController;
import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.model.Book;
import com.dynatrace.model.Client;
import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.repository.BookRepository;
import com.dynatrace.ratings.repository.ClientRepository;
import com.dynatrace.ratings.repository.ConfigRepository;
import com.dynatrace.ratings.repository.RatingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController extends HardworkingController {
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(RatingController.class);

    // get all ratings
    @GetMapping("")
    @Operation(summary = "Get all ratings")
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll(Sort.by(Sort.Direction.ASC, "isbn", "email", "createdAt"));
    }

    // get ratings by a client
    @GetMapping("/findByEmail")
    @Operation(summary = "Get all ratings for the given client")
    public List<Rating> getRatingsByEmail(@Parameter(name="email", description = "email of an existing client", example = "pbrown.gmail.com") @RequestParam String email) {
        return ratingRepository.findByEmail(email);
    }

    // get ratings for a book
    @GetMapping("/findByISBN")
    @Operation(summary = "Get all ratings for the given book")
    public List<Rating> getRatingsByISBN(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        return ratingRepository.findByEmail(isbn);
    }

    // get a rating by id
    @GetMapping("/{id}")
    @Operation(summary = "Get a rating by ID")
    public Rating getRatingById(@PathVariable Long id) {
        Optional<Rating> rating = ratingRepository.findById(id);
        if (rating.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Rating not found");
            logger.error(ex.getMessage());
            throw ex;
        }
        return rating.get();
    }

    // create a rating
    @PostMapping("")
    @Operation(summary = "Create a rating. This operation does not require client/book to exist")
    public Rating createRating(@RequestBody Rating rating) {
        simulateHardWork();
        simulateCrash();
        this.verifyBook(rating.getIsbn());
        this.verifyClient(rating.getEmail());
        logger.debug("Creating Rating for book " + rating.getIsbn() + " from user " + rating.getEmail());
        return ratingRepository.save(rating);
    }

    // update a rating
    @PutMapping("/{id}")
    @Operation(summary = "Update a rating")
    public Rating updateRatingById(@PathVariable Long id, @RequestBody Rating rating) {
        Optional<Rating> ratingDb = ratingRepository.findById(id);
        if (ratingDb.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Rating not found");
            logger.error(ex.getMessage());
            throw ex;
        } else if (rating.getId() != id || ratingDb.get().getId() != id) {
            BadRequestException ex = new BadRequestException("bad rating id");
            logger.error(ex.getMessage());
            throw ex;
        }
        return ratingRepository.save(rating);
    }

    // delete a rating by id
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a rating")
    public void deleteRatingById(@PathVariable Long id) {
        ratingRepository.deleteById(id);
    }

    // delete all ratings
    @DeleteMapping("/delete-all")
    @Operation(summary = "Delete all ratings")
    public void deleteAllRatings() {
        ratingRepository.truncateTable();
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }


    private void verifyClient(String email) {
        logger.info("Verifying client " + email);
        Client client = clientRepository.getClientByEmail(email);
        if (null == client) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Client is not found by email " + email);
            logger.error(ex.getMessage());
            throw ex;
        }
        Client[] clients = clientRepository.getAllClients();
        logger.debug(clients.toString());
    }

    private Book verifyBook(String isbn) {
        logger.info("Verifying book " + isbn);
        Book book = bookRepository.getBookByISBN(isbn);
        if (null == book) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found by isbn " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        Book[] books = bookRepository.getAllBooks();
        logger.debug(books.toString());
        return book;
    }
}
