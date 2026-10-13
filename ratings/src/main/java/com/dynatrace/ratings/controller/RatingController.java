package com.dynatrace.ratings.controller;

import com.dynatrace.controller.SecurityController;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.model.RatingRequest;
import com.dynatrace.ratings.model.RatingSummary;
import com.dynatrace.ratings.repository.ConfigRepository;
import com.dynatrace.ratings.repository.RatingRepository;
import com.dynatrace.ratings.service.RatingService;
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
public class RatingController extends SecurityController {
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private RatingService ratingService;
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
        return ratingRepository.findByIsbn(isbn);
    }

    // get a book's rating summary
    @GetMapping("/summary")
    @Operation(summary = "Get one book's rating count and average")
    public RatingSummary getSummary(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        return ratingService.summary(isbn);
    }

    // get the rating summary of every rated book
    @GetMapping("/summaries")
    @Operation(summary = "Get the rating count and average of every rated book")
    public List<RatingSummary> getSummaries() {
        return ratingService.summaries();
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

    // create a rating, or change the customer's existing rating of the book
    @PostMapping("")
    @Operation(summary = "Create a rating, or change the customer's existing rating of the book")
    public Rating createRating(@RequestBody RatingRequest request) {
        runThreatScan();
        applySecurityPolicy();
        Rating rating = ratingService.give(request);
        logger.debug("Rating " + rating.getId() + " saved for book " + rating.getIsbn());
        return rating;
    }

    // update a rating
    @PutMapping("/{id}")
    @Operation(summary = "Update a rating's stars and comment")
    public Rating updateRatingById(@PathVariable Long id, @RequestBody RatingRequest request) {
        return ratingService.change(id, request);
    }

    // delete a rating by id
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a rating")
    public void deleteRatingById(@PathVariable Long id) {
        ratingRepository.deleteById(id);
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
