package com.dynatrace.ratings.service;

import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.PurchaseForbiddenException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.model.RatingRequest;
import com.dynatrace.ratings.model.RatingSummary;
import com.dynatrace.ratings.purchase.InMemoryPurchaseCheck;
import com.dynatrace.ratings.purchase.OrdersUnavailableException;
import com.dynatrace.ratings.repository.RatingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
class RatingServiceTest {
    private static final String ISBN = "9780000000017";
    private static final String OTHER_ISBN = "9780000000024";
    private static final String READER = "reader@example.com";
    private static final Instant T0 = Instant.parse("2026-10-13T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-13T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-13T11:00:00Z");
    private static final Instant T3 = Instant.parse("2026-10-13T12:00:00Z");

    @Autowired
    private RatingService ratingService;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private InMemoryPurchaseCheck inMemoryPurchaseCheck;
    @Autowired
    private SteppedClock steppedClock;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAllInBatch();
        inMemoryPurchaseCheck.reset();
        steppedClock.set(T1);
    }

    // ---- helpers ----

    private RatingRequest req(String json) {
        try {
            return objectMapper.readValue(json, RatingRequest.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String giveJson(String email, String isbn, int stars, String comment) {
        return "{\"email\":\"" + email + "\",\"isbn\":\"" + isbn + "\",\"rating\":" + stars
                + ",\"comment\":\"" + comment + "\"}";
    }

    private static String changeJson(long id, int stars, String comment) {
        return "{\"id\":" + id + ",\"rating\":" + stars + ",\"comment\":\"" + comment + "\"}";
    }

    private Rating seed(String email, String isbn, int stars, String comment, Instant at) {
        return ratingRepository.save(new Rating(email, isbn, stars, comment, Date.from(at)));
    }

    private Rating stored(long id) {
        return ratingRepository.findById(id).orElseThrow();
    }

    private void assertBad(String reason, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertEquals(reason, ex.getReason());
    }

    private void assertNothingSavedNorAsked() {
        assertEquals(0, ratingRepository.count());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    // ---- give: validation ----

    @Test
    void giveWithBadEmailAndBadIsbnAndZeroStarsIsRefusedAsEmail() {
        assertBad(RatingService.INVALID_EMAIL, () -> ratingService.give(req(
                "{\"email\":\"reader-at-example\",\"isbn\":\"12\",\"rating\":0}")));
        assertNothingSavedNorAsked();
    }

    @Test
    void giveWithBadIsbnAndSixStarsIsRefusedAsIsbn() {
        assertBad(RatingService.INVALID_ISBN, () -> ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":\"12\",\"rating\":6}")));
        assertNothingSavedNorAsked();
    }

    @Test
    void giveWithNoIsbnIsRefusedAsIsbn() {
        assertBad(RatingService.INVALID_ISBN, () -> ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"rating\":4}")));
        assertNothingSavedNorAsked();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "true", "null", "[\"a@b.com\"]", "\"not-an-email\""})
    void giveWithNonEmailValueIsRefusedAsEmail(String emailJson) {
        assertBad(RatingService.INVALID_EMAIL, () -> ratingService.give(req(
                "{\"email\":" + emailJson + ",\"isbn\":\"" + ISBN + "\",\"rating\":4}")));
        assertNothingSavedNorAsked();
    }

    @Test
    void giveWithNoEmailIsRefusedAsEmail() {
        assertBad(RatingService.INVALID_EMAIL, () -> ratingService.give(req(
                "{\"isbn\":\"" + ISBN + "\",\"rating\":4}")));
        assertNothingSavedNorAsked();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"123456789012\"", "\"97800000000170\"", "\"978000000001x\"", "123456789012",
            "9.780000000017", "true"})
    void giveWithInvalidIsbnValueIsRefusedAsIsbn(String isbnJson) {
        assertBad(RatingService.INVALID_ISBN, () -> ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":" + isbnJson + ",\"rating\":4}")));
        assertNothingSavedNorAsked();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6", "-1", "3.5", "4.0", "\"4\"", "null", "true", "[4]",
            "4294967300", "-4294967291", "18446744073709551617"})
    void giveWithInvalidStarsIsRefusedAsStars(String starsJson) {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        assertBad(RatingService.INVALID_RATING, () -> ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":\"" + ISBN + "\",\"rating\":" + starsJson + "}")));
        assertEquals(0, ratingRepository.count());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void giveWithNoStarsIsRefusedNotStoredAsZero() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        assertBad(RatingService.INVALID_RATING, () -> ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":\"" + ISBN + "\"}")));
        assertNothingSavedNorAsked();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void giveWithOneOrFiveStarsIsStored(int stars) {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(giveJson(READER, ISBN, stars, "ok")));
        assertEquals(stars, stored(saved.getId()).getRating());
    }

    @Test
    void giveWithThirteenDigitIntegerIsbnIsStoredAsText() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":9780000000017,\"rating\":4}"));
        assertEquals(ISBN, stored(saved.getId()).getIsbn());
    }

    @Test
    void giveWithInvalidStarsAndNonBuyerIsRefusedAsStarsWithoutPurchaseCheck() {
        assertBad(RatingService.INVALID_RATING, () -> ratingService.give(req(giveJson(READER, ISBN, 0, "x"))));
        assertNothingSavedNorAsked();
    }

    // ---- give: buyers only ----

    @Test
    void giveByNonBuyerIsRefusedWithBuyersOnlyAndNothingIsSaved() {
        PurchaseForbiddenException ex = assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 4, "ok"))));
        assertEquals(RatingService.BUYERS_ONLY, ex.getReason());
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void giveByBuyerOfAnotherBookIsRefused() {
        inMemoryPurchaseCheck.order(READER, OTHER_ISBN, true);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 4, "ok"))));
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void giveWithUncompletedOrderIsRefused() {
        inMemoryPurchaseCheck.order(READER, ISBN, false);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 4, "ok"))));
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void purchaseIsCheckedUnderTheEmailAsTyped() {
        inMemoryPurchaseCheck.order("Reader@Example.com", ISBN, true);
        ratingService.give(req(giveJson("Reader@Example.com", ISBN, 4, "ok")));
        assertEquals(List.of("Reader@Example.com"), inMemoryPurchaseCheck.askedEmails());
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson("reader@example.com", OTHER_ISBN, 4, "ok"))));
    }

    @Test
    void giveWhileOrdersIsUnavailableIsRefusedAsUnavailableAndNothingIsSaved() {
        inMemoryPurchaseCheck.order(READER, ISBN, true).unavailable(true);
        assertThrows(OrdersUnavailableException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 4, "ok"))));
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void giveIsStoredOnceOrdersAnswersAgain() {
        inMemoryPurchaseCheck.order(READER, ISBN, true).unavailable(true);
        assertThrows(OrdersUnavailableException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 4, "ok"))));
        inMemoryPurchaseCheck.unavailable(false);
        ratingService.give(req(giveJson(READER, ISBN, 4, "ok")));
        assertEquals(1, ratingRepository.count());
    }

    @Test
    void giveIgnoresTheIdItCarriesAndLeavesAnotherRatingUntouched() {
        Rating other = seed("other@example.com", OTHER_ISBN, 2, "mine", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req("{\"id\":" + other.getId() + ",\"email\":\"" + READER
                + "\",\"isbn\":\"" + ISBN + "\",\"rating\":5,\"comment\":\"new\"}"));
        assertNotEquals(other.getId(), saved.getId());
        assertEquals(2, stored(other.getId()).getRating());
        assertEquals(2, ratingRepository.count());
    }

    @Test
    void giveCarryingIdZeroIsStoredAsNewRating() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req("{\"id\":0,\"email\":\"" + READER + "\",\"isbn\":\"" + ISBN
                + "\",\"rating\":5,\"comment\":\"new\"}"));
        assertTrue(saved.getId() > 0);
        assertEquals(1, ratingRepository.count());
    }

    // ---- give: comment on a new rating ----

    @Test
    void newGiveStoresTheComment() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(giveJson(READER, ISBN, 4, "Great plot")));
        assertEquals("Great plot", stored(saved.getId()).getComment());
    }

    @Test
    void newGiveKeepsLeadingSpacesOfTheComment() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(giveJson(READER, ISBN, 4, "   padded")));
        assertEquals("   padded", stored(saved.getId()).getComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\\t\\n\\t", "\\n"})
    void newGiveWithBlankCommentStoresNone(String comment) {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(giveJson(READER, ISBN, 4, comment)));
        assertNull(stored(saved.getId()).getComment());
    }

    @Test
    void newGiveWithNoCommentFieldStoresNone() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating saved = ratingService.give(req(
                "{\"email\":\"" + READER + "\",\"isbn\":\"" + ISBN + "\",\"rating\":4}"));
        assertNull(stored(saved.getId()).getComment());
    }

    // ---- give applied to an existing rating ----

    @Test
    void repeatGiveChangesTheExistingRatingInsteadOfAddingOne() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating answered = ratingService.give(req(giveJson(READER, ISBN, 5, "newer")));
        assertEquals(first.getId(), answered.getId());
        assertEquals(1, ratingRepository.count());
        assertEquals(5, stored(first.getId()).getRating());
    }

    @Test
    void repeatGiveReplacesTheStoredCommentAsGiven() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.give(req(giveJson(READER, ISBN, 5, "  replaced")));
        assertEquals("  replaced", stored(first.getId()).getComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\\t\\n\\t"})
    void repeatGiveWithBlankCommentKeepsTheStoredComment(String comment) {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.give(req(giveJson(READER, ISBN, 5, comment)));
        Rating after = stored(first.getId());
        assertEquals(5, after.getRating());
        assertEquals("old", after.getComment());
    }

    @Test
    void repeatGiveWithNoCommentFieldKeepsTheStoredComment() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.give(req("{\"email\":\"" + READER + "\",\"isbn\":\"" + ISBN + "\",\"rating\":5}"));
        assertEquals("old", stored(first.getId()).getComment());
    }

    @Test
    void repeatGiveAfterOrderWasCancelledIsRefusedAndLeavesTheRating() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, false);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 5, "new"))));
        Rating after = stored(first.getId());
        assertEquals(2, after.getRating());
        assertEquals("old", after.getComment());
    }

    @Test
    void repeatGiveWhileOrdersIsUnavailableLeavesTheRating() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true).unavailable(true);
        assertThrows(OrdersUnavailableException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 5, "new"))));
        assertEquals(2, stored(first.getId()).getRating());
    }

    @Test
    void giveFromAnotherBuyerOfTheSameBookIsARatingOfItsOwn() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order("second@example.com", ISBN, true);
        Rating answered = ratingService.give(req(giveJson("second@example.com", ISBN, 4, "mine")));
        assertNotEquals(first.getId(), answered.getId());
        assertEquals(2, ratingRepository.count());
        assertEquals(2, stored(first.getId()).getRating());
    }

    @Test
    void repeatGiveTypedInOtherCapitalsChangesTheSameRatingAndKeepsStoredEmail() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order("READER@EXAMPLE.COM", ISBN, true);
        Rating answered = ratingService.give(req(giveJson("READER@EXAMPLE.COM", ISBN, 5, "caps")));
        assertEquals(first.getId(), answered.getId());
        assertEquals(1, ratingRepository.count());
        Rating after = stored(first.getId());
        assertEquals(5, after.getRating());
        assertEquals(READER, after.getEmail());
    }

    // ---- change: validation and rules ----

    @Test
    void changeReplacesTheStarsAndTheComment() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating answered = ratingService.change(first.getId(), req(changeJson(first.getId(), 4, "new comment")));
        assertEquals(first.getId(), answered.getId());
        Rating after = stored(first.getId());
        assertEquals(4, after.getRating());
        assertEquals("new comment", after.getComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\\t\\n\\t"})
    void changeWithBlankCommentKeepsTheStoredComment(String comment) {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req(changeJson(first.getId(), 4, comment)));
        Rating after = stored(first.getId());
        assertEquals(4, after.getRating());
        assertEquals("old", after.getComment());
    }

    @Test
    void changeWithNoCommentFieldKeepsTheStoredComment() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId() + ",\"rating\":4}"));
        assertEquals("old", stored(first.getId()).getComment());
    }

    @Test
    void changeCarryingNoEmailOrIsbnIsCheckedUnderTheStoredEmail() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req(changeJson(first.getId(), 4, "c")));
        assertEquals(List.of(READER), inMemoryPurchaseCheck.askedEmails());
    }

    @Test
    void changeCarryingJsonNullEmailAndIsbnProceedsUnderStoredValues() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId()
                + ",\"email\":null,\"isbn\":null,\"rating\":3,\"comment\":\"c\"}"));
        assertEquals(3, stored(first.getId()).getRating());
        assertEquals(List.of(READER), inMemoryPurchaseCheck.askedEmails());
    }

    @Test
    void changeRepeatingTheStoredEmailAndIsbnProceeds() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId() + ",\"email\":\"" + READER
                + "\",\"isbn\":\"" + ISBN + "\",\"rating\":4,\"comment\":\"c\"}"));
        assertEquals(4, stored(first.getId()).getRating());
    }

    @Test
    void changeCarryingTheStoredIsbnAsIntegerProceeds() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId()
                + ",\"isbn\":9780000000017,\"rating\":4,\"comment\":\"c\"}"));
        assertEquals(4, stored(first.getId()).getRating());
    }

    @Test
    void changeBodyShapedLikeTheStorefrontsIsAcceptedAndKeepsCreatedAt() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId() + ",\"email\":\"" + READER
                + "\",\"isbn\":\"" + ISBN + "\",\"rating\":5,\"comment\":\"c\","
                + "\"createdAt\":\"2020-01-01T00:00:00.000+00:00\",\"updatedAt\":\"2020-01-01T00:00:00.000+00:00\"}"));
        Rating after = stored(first.getId());
        assertEquals(T0, after.getCreatedAt().toInstant());
        assertEquals(T1, after.getUpdatedAt().toInstant());
    }

    @Test
    void changeTypedInOtherCapitalsKeepsTheStoredEmail() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order("Reader@Example.com", ISBN, true);
        ratingService.change(first.getId(), req("{\"id\":" + first.getId()
                + ",\"email\":\"Reader@Example.com\",\"rating\":4,\"comment\":\"c\"}"));
        Rating after = stored(first.getId());
        assertEquals(4, after.getRating());
        assertEquals(READER, after.getEmail());
        assertEquals(List.of("Reader@Example.com"), inMemoryPurchaseCheck.askedEmails());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6", "-1", "3.5", "4.0", "\"4\"", "null",
            "4294967300", "-4294967291", "18446744073709551617"})
    void changeWithInvalidStarsIsRefusedAsStarsAndLeavesTheRating(String starsJson) {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        assertBad(RatingService.INVALID_RATING, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"rating\":" + starsJson + ",\"comment\":\"c\"}")));
        assertEquals(2, stored(first.getId()).getRating());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithNoStarsIsRefusedNotStoredAsZero() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.INVALID_RATING, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"comment\":\"c\"}")));
        assertEquals(2, stored(first.getId()).getRating());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithMalformedEmailIsRefusedAsEmail() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.INVALID_EMAIL, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"email\":\"reader-at-example\",\"rating\":4}")));
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithNonStringEmailIsRefusedAsEmail() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.INVALID_EMAIL, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"email\":123,\"rating\":4}")));
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithShortIsbnGetsTheIsbnReasonNotTheMoveReason() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.INVALID_ISBN, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"isbn\":\"12345678901\",\"rating\":4}")));
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithTwelveDigitIntegerIsbnIsRefusedAsIsbn() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.INVALID_ISBN, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"isbn\":978000000001,\"rating\":4}")));
    }

    @Test
    void changeToAnotherBookIsRefusedAsMove() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, OTHER_ISBN, true);
        assertBad(RatingService.MOVE_REFUSED, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"isbn\":\"" + OTHER_ISBN + "\",\"rating\":4}")));
        assertEquals(ISBN, stored(first.getId()).getIsbn());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeToAnotherCustomerIsRefusedAsMove() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order("someone@example.com", ISBN, true);
        assertBad(RatingService.MOVE_REFUSED, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"email\":\"someone@example.com\",\"rating\":4}")));
        assertEquals(READER, stored(first.getId()).getEmail());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeToAnUnknownIdIsNotFoundBeforeTheBodyIdIsCompared() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ratingService.change(999_999L, req(changeJson(5, 4, "c"))));
        assertEquals(RatingService.NOT_FOUND, ex.getReason());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeToAnUnknownIdWhileOrdersIsUnavailableIsNotFound() {
        inMemoryPurchaseCheck.unavailable(true);
        assertThrows(ResourceNotFoundException.class,
                () -> ratingService.change(999_999L, req(changeJson(999_999L, 4, "c"))));
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeBodyFaultIsAnsweredBeforeAnUnknownId() {
        assertBad(RatingService.INVALID_RATING,
                () -> ratingService.change(999_999L, req(changeJson(999_999L, 6, "c"))));
    }

    @Test
    void changeNamingAnotherRatingsIdInTheBodyIsRefusedAsBadId() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        Rating other = seed("other@example.com", ISBN, 3, "o", T0);
        assertBad(RatingService.BAD_ID, () -> ratingService.change(first.getId(),
                req(changeJson(other.getId(), 4, "c"))));
        assertEquals(2, stored(first.getId()).getRating());
        assertEquals(3, stored(other.getId()).getRating());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"ID\"", "3.7", "null", "true", "18446744073709551617"})
    void changeWithUnusableBodyIdIsRefusedAsBadId(String idJson) {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        // "ID" stands for the stored rating's own id written as a JSON string
        String body = "{\"id\":" + idJson.replace("ID", String.valueOf(first.getId()))
                + ",\"rating\":4,\"comment\":\"c\"}";
        assertBad(RatingService.BAD_ID, () -> ratingService.change(first.getId(), req(body)));
        assertEquals(2, stored(first.getId()).getRating());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithNoBodyIdIsRefusedAsBadId() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        assertBad(RatingService.BAD_ID, () -> ratingService.change(first.getId(),
                req("{\"rating\":4,\"comment\":\"c\"}")));
        assertEquals(2, stored(first.getId()).getRating());
        assertTrue(inMemoryPurchaseCheck.askedEmails().isEmpty());
    }

    @Test
    void changeWithSeveralFaultsReportsTheStarsFirst() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        Rating other = seed("other@example.com", ISBN, 3, "o", T0);
        assertBad(RatingService.INVALID_RATING, () -> ratingService.change(first.getId(),
                req("{\"id\":" + other.getId() + ",\"isbn\":\"" + OTHER_ISBN + "\",\"rating\":6}")));
    }

    @Test
    void changeAfterOrderWasCancelledIsRefusedAndLeavesTheRating() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, false);
        PurchaseForbiddenException ex = assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.change(first.getId(), req(changeJson(first.getId(), 5, "new"))));
        assertEquals(RatingService.BUYERS_ONLY, ex.getReason());
        Rating after = stored(first.getId());
        assertEquals(2, after.getRating());
        assertEquals("old", after.getComment());
    }

    @Test
    void changeAfterOrderWasDeletedIsRefused() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.customer(READER);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.change(first.getId(), req(changeJson(first.getId(), 5, "new"))));
        assertEquals(2, stored(first.getId()).getRating());
    }

    @Test
    void changeTypedUnderASpellingWithNoOrderIsRefused() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        assertThrows(PurchaseForbiddenException.class, () -> ratingService.change(first.getId(),
                req("{\"id\":" + first.getId() + ",\"email\":\"READER@example.com\",\"rating\":4}")));
        assertEquals(2, stored(first.getId()).getRating());
    }

    @Test
    void changeWhileOrdersIsUnavailableLeavesTheRating() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true).unavailable(true);
        assertThrows(OrdersUnavailableException.class,
                () -> ratingService.change(first.getId(), req(changeJson(first.getId(), 5, "new"))));
        Rating after = stored(first.getId());
        assertEquals(2, after.getRating());
        assertEquals("old", after.getComment());
    }

    @Test
    void refusedChangeLeavesTheStoredComment() {
        Rating first = seed(READER, ISBN, 2, "keep me", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, false);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.change(first.getId(), req(changeJson(first.getId(), 5, "replacement"))));
        assertEquals("keep me", stored(first.getId()).getComment());
    }

    @Test
    void changeRemovedWhileOrdersAnswersIsNotFound() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true).onCheck(() -> ratingRepository.deleteById(first.getId()));
        assertThrows(ResourceNotFoundException.class,
                () -> ratingService.change(first.getId(), req(changeJson(first.getId(), 5, "new"))));
        assertEquals(0, ratingRepository.count());
    }

    // ---- timestamps ----

    @Test
    void U02_AC07_TC01() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        steppedClock.set(T1);
        Rating given = ratingService.give(req(giveJson(READER, ISBN, 5, "first")));
        steppedClock.set(T2);
        ratingService.change(given.getId(), req(changeJson(given.getId(), 3, "changed")));
        Rating after = stored(given.getId());
        assertEquals(T1, after.getCreatedAt().toInstant());
        assertEquals(T2, after.getUpdatedAt().toInstant());
        assertEquals(3, after.getRating());
    }

    @Test
    void U02_AC07_TC02() {
        Rating lapsed = seed("lapsed@example.com", ISBN, 2, "old", T1);
        inMemoryPurchaseCheck.order("lapsed@example.com", ISBN, false);
        steppedClock.set(T2);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.change(lapsed.getId(), req(changeJson(lapsed.getId(), 5, "new"))));
        Rating after = stored(lapsed.getId());
        assertEquals(T1, after.getCreatedAt().toInstant());
        assertEquals(T1, after.getUpdatedAt().toInstant());
    }

    @Test
    void U02_AC07_TC03() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        steppedClock.set(T1);
        Rating given = ratingService.give(req(giveJson(READER, ISBN, 5, "first")));
        steppedClock.set(T2);
        Rating again = ratingService.give(req(giveJson(READER, ISBN, 2, "second")));
        assertEquals(given.getId(), again.getId());
        Rating after = stored(given.getId());
        assertEquals(T1, after.getCreatedAt().toInstant());
        assertEquals(T2, after.getUpdatedAt().toInstant());
        assertEquals(2, after.getRating());
    }

    @Test
    void U02_AC07_TC04() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        steppedClock.set(T1);
        Rating given = ratingService.give(req(giveJson(READER, ISBN, 4, "Great plot")));
        steppedClock.set(T2);
        ratingService.change(given.getId(), req(changeJson(given.getId(), 4, "Great plot")));
        steppedClock.set(T3);
        ratingService.change(given.getId(), req(changeJson(given.getId(), 4, "Great plot")));
        Rating after = stored(given.getId());
        assertEquals(4, after.getRating());
        assertEquals(T1, after.getCreatedAt().toInstant());
        assertEquals(T3, after.getUpdatedAt().toInstant());
    }

    @Test
    void newGiveSetsCreatedAtAndUpdatedAtToTheClock() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        steppedClock.set(T2);
        Rating given = ratingService.give(req(giveJson(READER, ISBN, 4, "c")));
        Rating after = stored(given.getId());
        assertEquals(T2, after.getCreatedAt().toInstant());
        assertEquals(T2, after.getUpdatedAt().toInstant());
    }

    // ---- summaries ----

    private void seedStars(String isbn, int... stars) {
        for (int i = 0; i < stars.length; i++) {
            seed("rater" + i + "@example.com", isbn, stars[i], null, T0);
        }
    }

    @Test
    void summaryAverageOfFourPointTwoFiveRoundsUpToFourPointThree() {
        seedStars(ISBN, 5, 4, 4, 4);
        RatingSummary summary = ratingService.summary(ISBN);
        assertEquals(ISBN, summary.isbn());
        assertEquals(4, summary.count());
        assertEquals(new BigDecimal("4.3"), summary.average());
    }

    @Test
    void summaryAverageOfExactlyOnePointZeroFiveRoundsUpToOnePointOne() {
        int[] stars = new int[20];
        java.util.Arrays.fill(stars, 1);
        stars[0] = 2;
        seedStars(ISBN, stars);
        RatingSummary summary = ratingService.summary(ISBN);
        assertEquals(20, summary.count());
        assertEquals(new BigDecimal("1.1"), summary.average());
    }

    @Test
    void summaryOfAWholeAverageKeepsOneDecimal() {
        seedStars(ISBN, 5, 4, 3);
        assertEquals(new BigDecimal("4.0"), ratingService.summary(ISBN).average());
    }

    @Test
    void summaryOfARepeatingAverageRoundsToOneDecimal() {
        seedStars(ISBN, 5, 5, 4);
        assertEquals(new BigDecimal("4.7"), ratingService.summary(ISBN).average());
    }

    @Test
    void summaryOfAnUnratedIsbnHasCountZeroAndNoAverage() {
        RatingSummary summary = ratingService.summary(OTHER_ISBN);
        assertEquals(OTHER_ISBN, summary.isbn());
        assertEquals(0, summary.count());
        assertNull(summary.average());
    }

    @Test
    void summaryCountsOnlyTheRequestedIsbn() {
        seedStars(ISBN, 5, 5);
        seedStars(OTHER_ISBN, 1);
        assertEquals(2, ratingService.summary(ISBN).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void summaryWithBlankIsbnIsRefused(String isbn) {
        assertBad(RatingService.ISBN_REQUIRED, () -> ratingService.summary(isbn));
    }

    @Test
    void summaryWithNullIsbnIsRefused() {
        assertBad(RatingService.ISBN_REQUIRED, () -> ratingService.summary(null));
    }

    @Test
    void summariesListEveryRatedBookInIsbnOrderAndOmitUnratedOnes() {
        seedStars("9780000000031", 4);
        seedStars(ISBN, 5, 4, 4, 4);
        List<RatingSummary> summaries = ratingService.summaries();
        assertEquals(2, summaries.size());
        assertEquals(ISBN, summaries.get(0).isbn());
        assertEquals(new BigDecimal("4.3"), summaries.get(0).average());
        assertEquals("9780000000031", summaries.get(1).isbn());
        assertEquals(1, summaries.get(1).count());
        assertEquals(ratingService.summary(ISBN), summaries.get(0));
    }

    @Test
    void summariesWithNoRatingsIsEmpty() {
        assertTrue(ratingService.summaries().isEmpty());
    }

    @Test
    void summaryCountsAGiveAChangeAndARemovalOnTheNextRequest() {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        Rating given = ratingService.give(req(giveJson(READER, ISBN, 5, "c")));
        assertEquals(new BigDecimal("5.0"), ratingService.summary(ISBN).average());
        ratingService.change(given.getId(), req(changeJson(given.getId(), 3, "c")));
        assertEquals(new BigDecimal("3.0"), ratingService.summary(ISBN).average());
        ratingRepository.deleteById(given.getId());
        assertEquals(0, ratingService.summary(ISBN).count());
    }

    @Test
    void refusedGiveChangesNoSummary() {
        seedStars(ISBN, 4);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.give(req(giveJson(READER, ISBN, 1, "c"))));
        assertEquals(1, ratingService.summary(ISBN).count());
    }

    @Test
    void ratingStillCountsAfterItsOrderIsCancelled() {
        seed("keeper@example.com", "9780000000031", 4, null, T0);
        inMemoryPurchaseCheck.order("keeper@example.com", "9780000000031", false);
        RatingSummary summary = ratingService.summary("9780000000031");
        assertEquals(1, summary.count());
        assertEquals(new BigDecimal("4.0"), summary.average());
    }

    @Test
    void ratingStillCountsAfterItsOrderIsDeleted() {
        seed("keeper2@example.com", "9780000000048", 5, null, T0);
        seed("another@example.com", "9780000000048", 3, null, T0);
        RatingSummary summary = ratingService.summary("9780000000048");
        assertEquals(2, summary.count());
        assertEquals(new BigDecimal("4.0"), summary.average());
    }

    @Test
    void refusedChangeLeavesTheRatingCountingAsItWas() {
        Rating lapsed = seed("lapsed2@example.com", "9780000000055", 2, null, T0);
        inMemoryPurchaseCheck.order("lapsed2@example.com", "9780000000055", false);
        assertThrows(PurchaseForbiddenException.class,
                () -> ratingService.change(lapsed.getId(), req(changeJson(lapsed.getId(), 5, "c"))));
        RatingSummary summary = ratingService.summary("9780000000055");
        assertEquals(1, summary.count());
        assertEquals(new BigDecimal("2.0"), summary.average());
    }

    // ---- the insert race ----

    @Test
    void giveLosingTheInsertRaceChangesTheWinnersRating() {
        assertRaceRecovers(READER);
    }

    @Test
    void giveLosingTheInsertRaceToADifferentLetterCaseChangesTheWinnersRating() {
        assertRaceRecovers("READER@EXAMPLE.COM");
    }

    private void assertRaceRecovers(String winnerEmail) {
        inMemoryPurchaseCheck.order(READER, ISBN, true);
        AtomicBoolean raced = new AtomicBoolean();
        Rating[] winner = new Rating[1];
        inMemoryPurchaseCheck.onCheck(() -> {
            if (raced.compareAndSet(false, true)) {
                winner[0] = seed(winnerEmail, ISBN, 5, "winner", T0);
            }
        });
        steppedClock.set(T2);

        Rating answered = ratingService.give(req(giveJson(READER, ISBN, 2, "loser")));

        assertNotNull(winner[0]);
        assertEquals(winner[0].getId(), answered.getId());
        assertEquals(1, ratingRepository.count());
        Rating after = stored(winner[0].getId());
        assertEquals(2, after.getRating());
        assertEquals(winnerEmail, after.getEmail());
        assertEquals(T0, after.getCreatedAt().toInstant());
        assertEquals(T2, after.getUpdatedAt().toInstant());
        assertEquals(T0, answered.getCreatedAt().toInstant());
        assertEquals(1, inMemoryPurchaseCheck.askedEmails().size());
    }

    @Test
    void giveWhoseExistingRatingIsRemovedWhileOrdersAnswersStoresANewRating() {
        Rating first = seed(READER, ISBN, 2, "old", T0);
        inMemoryPurchaseCheck.order(READER, ISBN, true).onCheck(() -> ratingRepository.deleteById(first.getId()));
        Rating answered = ratingService.give(req(giveJson(READER, ISBN, 4, "again")));
        assertNotEquals(first.getId(), answered.getId());
        assertEquals(1, ratingRepository.count());
        assertEquals(4, stored(answered.getId()).getRating());
    }
}
