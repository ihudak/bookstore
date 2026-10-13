package com.dynatrace.ratings.service;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.PurchaseForbiddenException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.model.RatingRequest;
import com.dynatrace.ratings.model.RatingSummary;
import com.dynatrace.ratings.purchase.Eligibility;
import com.dynatrace.ratings.purchase.PurchaseCheck;
import com.dynatrace.ratings.repository.IsbnTotals;
import com.dynatrace.ratings.repository.RatingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

// Owns every rating rule: validation, buyers only, one rating per e-mail and ISBN, the change rules
// and the summaries. The purchase check runs outside any transaction, so a slow orders holds no connection.
@Service
public class RatingService {
    static final String INVALID_EMAIL = "Invalid email address";
    static final String INVALID_ISBN = "ISBN must be a 13-digits value";
    static final String INVALID_RATING = "Invalid rating. Must be a whole number from 1 to 5.";
    static final String BUYERS_ONLY = "Only customers who have bought this book can rate it.";
    static final String MOVE_REFUSED = "A rating's e-mail and ISBN cannot be changed.";
    static final String BAD_ID = "bad rating id";
    static final String NOT_FOUND = "Rating not found";
    static final String ISBN_REQUIRED = "isbn is required";

    private static final Pattern EMAIL = Pattern.compile(Rating.EMAIL_PATTERN);
    private static final Pattern ISBN = Pattern.compile("^\\d{13}$");

    private final Logger refusals = LoggerFactory.getLogger("com.dynatrace.ratings.refusals");

    private final RatingRepository ratingRepository;
    private final PurchaseCheck purchaseCheck;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RatingService(RatingRepository ratingRepository, PurchaseCheck purchaseCheck,
                         TransactionTemplate transactionTemplate, Clock clock) {
        this.ratingRepository = ratingRepository;
        this.purchaseCheck = purchaseCheck;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    // POST: a new rating, or the change of the pair's existing rating
    public Rating give(RatingRequest request) {
        String email = validEmail(request.email());
        String isbn = validIsbn(request.isbn());
        int stars = validStars(request.rating());

        Optional<Rating> existing = ratingRepository.findByEmailAndIsbn(email, isbn);
        requireBuyer(email, isbn);

        if (existing.isPresent()) {
            Rating changed = transactionTemplate.execute(status -> ratingRepository.findById(existing.get().getId())
                    .map(rating -> applyChange(rating, stars, request.comment()))
                    .orElse(null));
            if (changed != null) {
                return changed;
            }
            // removed meanwhile: the give stores a new rating
        }
        try {
            return transactionTemplate.execute(status ->
                    ratingRepository.saveAndFlush(new Rating(email, isbn, stars, request.comment(), now())));
        } catch (DataIntegrityViolationException ex) {
            // another give for the pair won the insert: apply this one to it as a change
            Rating changed = transactionTemplate.execute(status -> ratingRepository.findByEmailAndIsbn(email, isbn)
                    .map(rating -> applyChange(rating, stars, request.comment()))
                    .orElse(null));
            if (changed == null) {
                throw ex;
            }
            return changed;
        }
    }

    // PUT: only the stars and the comment change
    public Rating change(long id, RatingRequest request) {
        int stars = validStars(request.rating());
        String email = present(request.email()) ? validEmail(request.email()) : null;
        String isbn = present(request.isbn()) ? validIsbn(request.isbn()) : null;

        Rating stored = ratingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        if (!present(request.id()) || !request.id().isIntegralNumber() || !request.id().canConvertToLong()
                || request.id().longValue() != id) {
            throw new BadRequestException(BAD_ID);
        }
        if ((email != null && !email.equalsIgnoreCase(stored.getEmail()))
                || (isbn != null && !isbn.equals(stored.getIsbn()))) {
            throw new BadRequestException(MOVE_REFUSED);
        }

        requireBuyer(email != null ? email : stored.getEmail(), stored.getIsbn());

        return transactionTemplate.execute(status -> ratingRepository.findById(id)
                .map(rating -> applyChange(rating, stars, request.comment()))
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND)));
    }

    public RatingSummary summary(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            throw new BadRequestException(ISBN_REQUIRED);
        }
        return ratingRepository.totalsFor(isbn)
                .map(RatingService::toSummary)
                .orElse(new RatingSummary(isbn, 0, null));
    }

    public List<RatingSummary> summaries() {
        return ratingRepository.totalsForAll().stream().map(RatingService::toSummary).toList();
    }

    private void requireBuyer(String email, String isbn) {
        Eligibility eligibility = purchaseCheck.check(email, isbn);
        if (eligibility != Eligibility.ELIGIBLE) {
            refusals.info("Rating refused: isbn={} reason={}", isbn, eligibility);
            throw new PurchaseForbiddenException(BUYERS_ONLY);
        }
    }

    private Rating applyChange(Rating rating, int stars, String comment) {
        rating.applyChange(stars, comment, now());
        return ratingRepository.saveAndFlush(rating);
    }

    private Date now() {
        return Date.from(clock.instant());
    }

    private static RatingSummary toSummary(IsbnTotals totals) {
        BigDecimal average = BigDecimal.valueOf(totals.sum())
                .divide(BigDecimal.valueOf(totals.count()), 1, RoundingMode.HALF_UP);
        return new RatingSummary(totals.isbn(), totals.count(), average);
    }

    private static boolean present(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode();
    }

    private static String validEmail(JsonNode node) {
        if (!present(node) || !node.isTextual() || !EMAIL.matcher(node.textValue()).matches()) {
            throw new BadRequestException(INVALID_EMAIL);
        }
        return node.textValue();
    }

    // an ISBN may come as a JSON string or as a 13-digit JSON integer
    private static String validIsbn(JsonNode node) {
        if (!present(node) || !(node.isTextual() || node.isIntegralNumber())) {
            throw new BadRequestException(INVALID_ISBN);
        }
        String isbn = node.isTextual() ? node.textValue() : node.asText();
        if (!ISBN.matcher(isbn).matches()) {
            throw new BadRequestException(INVALID_ISBN);
        }
        return isbn;
    }

    // only a JSON integer from 1 to 5: 4.0, "4" and 3.5 are refused
    private static int validStars(JsonNode node) {
        if (!present(node) || !node.isIntegralNumber() || !node.canConvertToInt()
                || node.intValue() < 1 || node.intValue() > 5) {
            throw new BadRequestException(INVALID_RATING);
        }
        return node.intValue();
    }
}
