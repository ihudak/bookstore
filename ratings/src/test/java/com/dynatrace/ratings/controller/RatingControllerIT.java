package com.dynatrace.ratings.controller;

import com.dynatrace.ratings.model.Rating;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// [U01] a customer rates a book they bought, and [U05] an operator removes a single rating
class RatingControllerIT extends RatingControllerTestBase {
    private static final String BASE = "/api/v1/ratings";
    private static final String ISBN = "9780000000017";
    private static final String BUYERS_ONLY = "Only customers who have bought this book can rate it.";
    private static final String BAD_STARS = "Invalid rating. Must be a whole number from 1 to 5.";
    private static final String BAD_EMAIL = "Invalid email address";
    private static final String BAD_ISBN = "ISBN must be a 13-digits value";

    // ---------------------------------------------------------------- helpers

    private static String q(String text) {
        return "\"" + text + "\"";
    }

    private static String obj(String... fields) {
        return "{" + String.join(",", fields) + "}";
    }

    // a give body; a null argument leaves the field out, stars is a raw JSON literal
    private static String give(String email, String isbn, String stars) {
        List<String> fields = new ArrayList<>();
        if (email != null) {
            fields.add("\"email\":" + q(email));
        }
        if (isbn != null) {
            fields.add("\"isbn\":" + q(isbn));
        }
        if (stars != null) {
            fields.add("\"rating\":" + stars);
        }
        return obj(fields.toArray(new String[0]));
    }

    private ResultActions post(String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(BASE).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(long id, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions get(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url));
    }

    private ResultActions delete(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url));
    }

    private static String raw(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(raw(actions));
    }

    private ResultActions expectError(ResultActions actions, int code, String reason, String message, String path) throws Exception {
        return actions.andExpect(status().is(code))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.error").value(reason))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private ResultActions expectBadRequest(ResultActions actions, String message) throws Exception {
        return expectError(actions, 400, "Bad Request", message, BASE);
    }

    private ResultActions expectBuyersOnly(ResultActions actions) throws Exception {
        return expectError(actions, 403, "Forbidden", BUYERS_ONLY, BASE);
    }

    private ResultActions expectUnavailable(ResultActions actions) throws Exception {
        return expectError(actions, 503, "Service Unavailable", "Service Unavailable", BASE);
    }

    // a successful answer carries no comment key and does not repeat the comment text
    private static void assertNoComment(String body, String commentText) {
        assertFalse(body.contains("\"comment\""), "answer has a comment key: " + body);
        if (commentText != null) {
            assertFalse(body.contains(commentText), "answer repeats the comment: " + body);
        }
    }

    private ObjectNode withoutTimestamp(String body) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(body);
        node.remove("timestamp");
        return node;
    }

    private static List<String> refusalLines(CapturedOutput output) {
        return output.getAll().lines().filter(line -> line.contains("Rating refused")).toList();
    }

    private static void assertRefusalLogged(CapturedOutput output, String isbn, String reason) {
        List<String> lines = refusalLines(output);
        assertEquals(1, lines.size(), "refusal entries: " + lines);
        String line = lines.get(0);
        assertTrue(line.contains("com.dynatrace.ratings.refusals"), line);
        assertTrue(line.contains("isbn=" + isbn), line);
        assertTrue(line.contains("reason=" + reason), line);
        assertFalse(line.contains("@"), "refusal entry holds an e-mail: " + line);
    }

    private static String order(String isbn, boolean completed) {
        return "{\"id\":1,\"isbn\":" + q(isbn) + ",\"quantity\":1,\"price\":10.0,\"completed\":" + completed + "}";
    }

    private List<Rating> seedFive() {
        List<Rating> seeded = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            seeded.add(seed("reader" + i + "@example.com", ISBN, i, null));
        }
        return seeded;
    }

    // ---------------------------------------------------------------- [U01] [AC01]

    @Test
    void U01_AC01_TC01() throws Exception {
        ordersBuyer("reader@example.com", ISBN);

        post(give("reader@example.com", ISBN, "4")).andExpect(status().isOk());

        get(BASE + "/findByEmail?email=reader@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("reader@example.com"))
                .andExpect(jsonPath("$[0].isbn").value(ISBN))
                .andExpect(jsonPath("$[0].rating").value(4));
        assertEquals(1, ratingRepository.count());
    }

    @Test
    void U01_AC01_TC02() throws Exception {
        ordersBuyer("reader2@example.com", ISBN);

        expectBuyersOnly(post(give("reader2@example.com", "9780000000031", "4")));

        get(BASE + "/findByEmail?email=reader2@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertTrue(ratingRepository.findByEmailAndIsbn("reader2@example.com", "9780000000031").isEmpty());
    }

    @Test
    void U01_AC01_TC03() throws Exception {
        // orders holds the order under Reader9@Example.com only; the give is checked as typed
        ordersAnswer("reader9@example.com", "[]");

        expectBuyersOnly(post(give("reader9@example.com", ISBN, "4")));

        get(BASE + "/findByISBN?isbn=" + ISBN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- [U01] [AC02]

    @Test
    void U01_AC02_TC01() throws Exception {
        ordersBuyer("direct@example.com", "9780000000024");
        String comment = "Loved every chapter of it";

        ResultActions result = post(obj("\"email\":\"direct@example.com\"", "\"isbn\":\"9780000000024\"",
                "\"rating\":5", "\"comment\":" + q(comment)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("direct@example.com"))
                .andExpect(jsonPath("$.isbn").value("9780000000024"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        assertNoComment(raw(result), comment);
        long id = json(result).get("id").asLong();
        assertEquals(5, stored(id).getRating());
    }

    @Test
    void U01_AC02_TC02() throws Exception {
        ordersAnswer("browser@example.com", "[]");

        ResultActions result = post(give("browser@example.com", ISBN, "4"));

        result.andExpect(status().isForbidden());
        assertFalse(json(result).has("id"));
        assertTrue(raw(result).indexOf("\"id\"") < 0);
    }

    // ---------------------------------------------------------------- [U01] [AC03]

    @Test
    void U01_AC03_TC01() throws Exception {
        ordersAnswer("browser@example.com", "[]");

        expectBuyersOnly(post(give("browser@example.com", ISBN, "4")));

        get(BASE + "/findByEmail?email=browser@example.com").andExpect(jsonPath("$.length()").value(0));
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC03_TC02() throws Exception {
        ordersAnswer("unpaid@example.com", "[" + order(ISBN, false) + "]");

        expectBuyersOnly(post(give("unpaid@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC03_TC03() throws Exception {
        ordersAnswer("cancelled@example.com", "[" + order(ISBN, false) + "]");

        expectBuyersOnly(post(give("cancelled@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC03_TC04() throws Exception {
        ordersAnswer("deleted@example.com", "[]");

        expectBuyersOnly(post(give("deleted@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC03_TC05() throws Exception {
        ordersNotACustomer("stranger@example.com");

        expectBuyersOnly(post(give("stranger@example.com", ISBN, "4")));

        assertTrue(ratingRepository.findByEmail("stranger@example.com").isEmpty());
    }

    @Test
    void U01_AC03_TC06() throws Exception {
        ordersBuyer("reader@example.com", ISBN);

        expectBuyersOnly(post(give("reader@example.com", "9789999999999", "4")));

        assertTrue(ratingRepository.findByIsbn("9789999999999").isEmpty());
    }

    // ---------------------------------------------------------------- [U01] [AC04]

    @Test
    void U01_AC04_TC01() throws Exception {
        ordersNotACustomer("stranger@example.com");
        ordersAnswer("browser@example.com", "[]");

        ResultActions unknown = expectBuyersOnly(post(give("stranger@example.com", ISBN, "4")));
        ResultActions noOrder = expectBuyersOnly(post(give("browser@example.com", ISBN, "4")));

        assertEquals(withoutTimestamp(raw(unknown)), withoutTimestamp(raw(noOrder)));
    }

    @Test
    void U01_AC04_TC02() throws Exception {
        ordersAnswer("unpaid@example.com", "[" + order(ISBN, false) + "]");
        ordersAnswer("cancelled@example.com", "[" + order(ISBN, false) + "]");
        ordersAnswer("deleted@example.com", "[]");
        ordersBuyer("reader@example.com", ISBN);

        ObjectNode unpaid = withoutTimestamp(raw(expectBuyersOnly(post(give("unpaid@example.com", ISBN, "4")))));
        ObjectNode cancelled = withoutTimestamp(raw(expectBuyersOnly(post(give("cancelled@example.com", ISBN, "4")))));
        ObjectNode deleted = withoutTimestamp(raw(expectBuyersOnly(post(give("deleted@example.com", ISBN, "4")))));
        ObjectNode unknownBook = withoutTimestamp(raw(expectBuyersOnly(post(give("reader@example.com", "9789999999999", "4")))));

        assertEquals(unpaid, cancelled);
        assertEquals(unpaid, deleted);
        assertEquals(unpaid, unknownBook);
    }

    @Test
    void U01_AC04_TC03() throws Exception {
        ordersAnswer("cancelled@example.com", "[" + order(ISBN, false) + "]");

        String body = raw(expectBuyersOnly(post(give("cancelled@example.com", ISBN, "4"))));

        JsonNode node = objectMapper.readTree(body);
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        assertEquals(List.of("timestamp", "status", "error", "message", "path"), keys);
        assertFalse(body.contains("NOT_A_CUSTOMER"));
        assertFalse(body.contains("NO_ORDER_FOR_BOOK"));
        assertFalse(body.contains("ORDER_NOT_COMPLETED"));
        assertFalse(body.contains("cancelled"));
        assertFalse(body.contains("completed"));
    }

    @Test
    void U01_AC04_TC04() throws Exception {
        Rating lapsed = seed("lapsed@example.com", ISBN, 3, null);
        ordersAnswer("lapsed@example.com", "[" + order(ISBN, false) + "]");
        ordersAnswer("browser@example.com", "[]");

        ResultActions change = expectError(put(lapsed.getId(), obj("\"id\":" + lapsed.getId(), "\"rating\":5")),
                403, "Forbidden", BUYERS_ONLY, BASE + "/" + lapsed.getId());
        ResultActions newGive = expectBuyersOnly(post(give("browser@example.com", ISBN, "4")));

        ObjectNode changeBody = withoutTimestamp(raw(change));
        ObjectNode giveBody = withoutTimestamp(raw(newGive));
        changeBody.remove("path");
        giveBody.remove("path");
        assertEquals(giveBody, changeBody);
        assertEquals(3, stored(lapsed.getId()).getRating());
    }

    // ---------------------------------------------------------------- [U01] [AC05]

    @Test
    void U01_AC05_TC01() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("nostars@example.com", ISBN, null)), BAD_STARS);

        get(BASE + "/findByEmail?email=nostars@example.com").andExpect(jsonPath("$.length()").value(0));
        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC05_TC02() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("edges@example.com", ISBN, "0")), BAD_STARS);
        expectBadRequest(post(give("edges@example.com", ISBN, "6")), BAD_STARS);
        expectBadRequest(post(give("edges@example.com", ISBN, "-1")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC05_TC03() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("fraction@example.com", ISBN, "3.5")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC05_TC04() throws Exception {
        ordersBuyer("low@example.com", ISBN);
        ordersBuyer("high@example.com", ISBN);

        ResultActions low = post(give("low@example.com", ISBN, "1")).andExpect(status().isOk());
        ResultActions high = post(give("high@example.com", ISBN, "5")).andExpect(status().isOk());

        assertEquals(1, stored(json(low).get("id").asLong()).getRating());
        assertEquals(5, stored(json(high).get("id").asLong()).getRating());
        assertEquals(1, ratingRepository.findByEmailAndIsbn("low@example.com", ISBN).orElseThrow().getRating());
        assertEquals(5, ratingRepository.findByEmailAndIsbn("high@example.com", ISBN).orElseThrow().getRating());
    }

    @Test
    void U01_AC05_TC05() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("typed@example.com", ISBN, "4.0")), BAD_STARS);
        expectBadRequest(post(give("typed@example.com", ISBN, "\"4\"")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC06]

    @Test
    void U01_AC06_TC01() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("reader-at-example", ISBN, "4")), BAD_EMAIL);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC06_TC02() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("reader@example.com", "978000000001", "4")), BAD_ISBN);
        expectBadRequest(post(give("reader@example.com", "97800000000170", "4")), BAD_ISBN);
        expectBadRequest(post(give("reader@example.com", "978000000001X", "4")), BAD_ISBN);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC06_TC03() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give(null, ISBN, "4")), BAD_EMAIL);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC06_TC04() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("reader@example.com", null, "4")), BAD_ISBN);

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC07]

    @Test
    void U01_AC07_TC01() throws Exception {
        ordersNeverCalled();

        post(give("reader-at-example", ISBN, "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", anyOf(is(BAD_EMAIL), is(BAD_STARS))));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC07_TC02() throws Exception {
        ordersNeverCalled();

        post(give("reader@example.com", null, "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", anyOf(is(BAD_ISBN), is(BAD_STARS))));

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC08]

    @Test
    void U01_AC08_TC01() throws Exception {
        // browser would be refused 403 if the give reached the purchase check
        ordersNeverCalled();

        expectBadRequest(post(give("browser@example.com", ISBN, "0")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC08_TC02() throws Exception {
        // orders would fail with 503 if the give reached the purchase check
        ordersNeverCalled();

        expectBadRequest(post(give("reader-at-example", ISBN, "4")), BAD_EMAIL);

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC09]

    @Test
    void U01_AC09_TC01() throws Exception {
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("reader5@example.com")))
                .andRespond(withException(new ConnectException("refused")));

        expectUnavailable(post(give("reader5@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC09_TC02() throws Exception {
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("browser@example.com")))
                .andRespond(withException(new ConnectException("refused")));

        expectUnavailable(post(give("browser@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC09_TC04() throws Exception {
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("browser@example.com")))
                .andRespond(withServerError());

        expectUnavailable(post(give("browser@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U01_AC09_TC05() throws Exception {
        // the failure is registered first, so orders fails the first give and answers the second
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("reader6@example.com")))
                .andRespond(withException(new ConnectException("refused")));
        ordersBuyer("reader6@example.com", ISBN);

        expectUnavailable(post(give("reader6@example.com", ISBN, "4")));
        assertEquals(0, ratingRepository.count());

        post(give("reader6@example.com", ISBN, "4")).andExpect(status().isOk());

        assertEquals(1, ratingRepository.count());
        assertEquals(4, ratingRepository.findByEmailAndIsbn("reader6@example.com", ISBN).orElseThrow().getRating());
    }

    @Test
    void U01_AC09_TC06() throws Exception {
        ordersAnswer("browser@example.com", "{\"unexpected\":\"not a list of orders\"}");

        expectUnavailable(post(give("browser@example.com", ISBN, "4")));

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC10]

    @Test
    void U01_AC10_TC01() throws Exception {
        Rating second = seed("second@example.com", ISBN, 3, null);
        ordersBuyer("reader10@example.com", "9780000000024");

        ResultActions result = post(obj("\"id\":" + second.getId(), "\"email\":\"reader10@example.com\"",
                "\"isbn\":\"9780000000024\"", "\"rating\":4")).andExpect(status().isOk());

        long newId = json(result).get("id").asLong();
        assertNotEquals(second.getId(), newId);
        Rating created = stored(newId);
        assertEquals("reader10@example.com", created.getEmail());
        assertEquals("9780000000024", created.getIsbn());
        assertEquals(4, created.getRating());
        get(BASE + "/findByISBN?isbn=9780000000024").andExpect(jsonPath("$.length()").value(1));
        Rating untouched = stored(second.getId());
        assertEquals(3, untouched.getRating());
        assertEquals("second@example.com", untouched.getEmail());
        assertEquals(ISBN, untouched.getIsbn());
    }

    @Test
    void U01_AC10_TC02() throws Exception {
        Rating own = seed("reader@example.com", ISBN, 2, null);
        Rating other = seed("second@example.com", ISBN, 3, null);
        ordersBuyer("reader@example.com", ISBN);

        ResultActions result = post(obj("\"id\":" + other.getId(), "\"email\":\"reader@example.com\"",
                "\"isbn\":" + q(ISBN), "\"rating\":5")).andExpect(status().isOk());

        assertEquals(own.getId(), json(result).get("id").asLong());
        assertEquals(5, stored(own.getId()).getRating());
        Rating untouched = stored(other.getId());
        assertEquals(3, untouched.getRating());
        assertEquals("second@example.com", untouched.getEmail());
        assertEquals(2, ratingRepository.count());
    }

    @Test
    void U01_AC10_TC03() throws Exception {
        ordersBuyer("reader11@example.com", ISBN);

        post(obj("\"id\":0", "\"email\":\"reader11@example.com\"", "\"isbn\":" + q(ISBN), "\"rating\":4"))
                .andExpect(status().isOk());

        get(BASE + "/findByEmail?email=reader11@example.com")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].isbn").value(ISBN))
                .andExpect(jsonPath("$[0].rating").value(4));
        assertEquals(1, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U01] [AC11]

    @Test
    void U01_AC11_TC01(CapturedOutput output) throws Exception {
        ordersAnswer("unpaid@example.com", "[" + order(ISBN, false) + "]");

        expectBuyersOnly(post(give("unpaid@example.com", ISBN, "4")));

        assertRefusalLogged(output, ISBN, "ORDER_NOT_COMPLETED");
    }

    @Test
    void U01_AC11_TC02(CapturedOutput output) throws Exception {
        ordersNotACustomer("stranger@example.com");

        expectBuyersOnly(post(give("stranger@example.com", ISBN, "4")));

        assertRefusalLogged(output, ISBN, "NOT_A_CUSTOMER");
    }

    @Test
    void U01_AC11_TC03(CapturedOutput output) throws Exception {
        ordersAnswer("browser@example.com", "[]");

        expectBuyersOnly(post(give("browser@example.com", ISBN, "4")));

        assertRefusalLogged(output, ISBN, "NO_ORDER_FOR_BOOK");
    }

    @Test
    void U01_AC11_TC04(CapturedOutput output) throws Exception {
        Rating lapsed = seed("lapsed@example.com", ISBN, 3, null);
        ordersAnswer("lapsed@example.com", "[" + order(ISBN, false) + "]");

        expectError(put(lapsed.getId(), obj("\"id\":" + lapsed.getId(), "\"rating\":5")),
                403, "Forbidden", BUYERS_ONLY, BASE + "/" + lapsed.getId());

        assertRefusalLogged(output, ISBN, "ORDER_NOT_COMPLETED");
    }

    @Test
    void U01_AC11_TC05(CapturedOutput output) throws Exception {
        ordersAnswer("cancelled@example.com", "[" + order(ISBN, false) + "]");

        expectBuyersOnly(post(give("cancelled@example.com", ISBN, "4")));

        List<String> lines = refusalLines(output);
        assertEquals(1, lines.size(), "refusal entries: " + lines);
        assertTrue(lines.get(0).contains(ISBN));
        assertTrue(lines.get(0).contains("ORDER_NOT_COMPLETED"));
        assertFalse(lines.get(0).contains("cancelled@example.com"));
        assertFalse(lines.get(0).contains("@"));
        assertFalse(lines.get(0).contains("%40"));
    }

    @Test
    void U01_AC11_TC06(CapturedOutput output) throws Exception {
        ordersAnswer("cancelled@example.com", "[" + order(ISBN, false) + "]");

        String body = raw(expectBuyersOnly(post(give("cancelled@example.com", ISBN, "4"))));

        assertRefusalLogged(output, ISBN, "ORDER_NOT_COMPLETED");
        assertFalse(body.contains("ORDER_NOT_COMPLETED"));
        assertFalse(body.contains("reason"));
    }

    // ---------------------------------------------------------------- give: further cases

    @Test
    void giveAfterOperatorRemovedTheRatingInsertsANewRating() throws Exception {
        Rating original = seed("racer@example.com", ISBN, 3, null);
        String buyerAnswer = "[{\"id\":1,\"isbn\":" + q(ISBN) + ",\"completed\":true}]";
        ResponseCreator removeThenAnswer = request -> {
            ratingRepository.deleteById(original.getId());
            return withSuccess(buyerAnswer, MediaType.APPLICATION_JSON).createResponse(request);
        };
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("racer@example.com"))).andRespond(removeThenAnswer);

        ResultActions result = post(give("racer@example.com", ISBN, "5")).andExpect(status().isOk());

        long newId = json(result).get("id").asLong();
        assertNotEquals(original.getId(), newId);
        assertEquals(5, stored(newId).getRating());
        assertEquals(1, ratingRepository.count());
        assertTrue(ratingRepository.findById(original.getId()).isEmpty());
    }

    @Test
    void starsBeyondTheIntRangeAreRefusedAndNeverWrapIntoTheScale() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(give("wrap@example.com", ISBN, "4294967300")), BAD_STARS);
        expectBadRequest(post(give("wrap@example.com", ISBN, "-4294967291")), BAD_STARS);
        expectBadRequest(post(give("wrap@example.com", ISBN, "18446744073709551617")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void aNullEmailIsRefusedAsAnInvalidEmail() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(obj("\"email\":null", "\"isbn\":" + q(ISBN), "\"rating\":4")), BAD_EMAIL);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void aNullIsbnIsRefusedAsAnInvalidIsbn() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(obj("\"email\":\"reader@example.com\"", "\"isbn\":null", "\"rating\":4")), BAD_ISBN);

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void aNullRatingIsRefusedAsInvalidStars() throws Exception {
        ordersNeverCalled();

        expectBadRequest(post(obj("\"email\":\"reader@example.com\"", "\"isbn\":" + q(ISBN), "\"rating\":null")), BAD_STARS);

        assertEquals(0, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U05] [AC01]

    @Test
    void U05_AC01_TC01() throws Exception {
        Rating reader = seed("reader@example.com", ISBN, 4, null);
        Rating second = seed("second@example.com", ISBN, 3, null);

        MvcResult removal = delete(BASE + "/" + reader.getId()).andExpect(status().isOk()).andReturn();

        assertEquals("", removal.getResponse().getContentAsString());
        get(BASE + "/findByISBN?isbn=" + ISBN)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[0].email").value("second@example.com"));
        assertTrue(ratingRepository.findById(reader.getId()).isEmpty());
    }

    @Test
    void U05_AC01_TC02() throws Exception {
        List<Rating> mine = List.of(
                seed("erase@example.com", "9780000000017", 4, null),
                seed("erase@example.com", "9780000000024", 5, null),
                seed("erase@example.com", "9780000000031", 2, null));
        seed("keep@example.com", "9780000000017", 3, null);

        for (Rating rating : mine) {
            delete(BASE + "/" + rating.getId()).andExpect(status().isOk());
        }

        get(BASE + "/findByEmail?email=erase@example.com").andExpect(jsonPath("$.length()").value(0));
        assertEquals(1, ratingRepository.count());
    }

    @Test
    void U05_AC01_TC03() throws Exception {
        Rating reader = seed("reader@example.com", ISBN, 4, null);
        Rating second = seed("second@example.com", ISBN, 3, null);

        delete(BASE + "/" + reader.getId()).andExpect(status().isOk());
        delete(BASE + "/" + reader.getId()).andExpect(status().isOk());

        get(BASE + "/findByISBN?isbn=" + ISBN)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(second.getId()));
    }

    // ---------------------------------------------------------------- [U05] [AC02]

    @Test
    void U05_AC02_TC01() throws Exception {
        seed("reader@example.com", ISBN, 4, null);
        seed("second@example.com", ISBN, 3, null);

        MvcResult removal = delete(BASE + "/999999").andExpect(status().isOk()).andReturn();

        assertEquals("", removal.getResponse().getContentAsString());
        get(BASE).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void U05_AC02_TC02() throws Exception {
        Rating rating = seed("reader@example.com", ISBN, 4, null);

        MvcResult first = delete(BASE + "/" + rating.getId()).andExpect(status().isOk()).andReturn();
        MvcResult again = delete(BASE + "/" + rating.getId()).andExpect(status().isOk()).andReturn();

        assertEquals("", first.getResponse().getContentAsString());
        assertEquals("", again.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------- [U05] [AC03]

    @Test
    void U05_AC03_TC01() throws Exception {
        seedFive();

        MvcResult result = delete(BASE + "/delete-all").andExpect(status().isBadRequest()).andReturn();

        assertInstanceOf(MethodArgumentTypeMismatchException.class, result.getResolvedException());
        assertEquals("", result.getResponse().getContentAsString());
        get(BASE).andExpect(jsonPath("$.length()").value(5));
        assertEquals(5, ratingRepository.count());
    }

    @Test
    void U05_AC03_TC02() throws Exception {
        seedFive();

        MvcResult result = delete(BASE + "/all").andExpect(status().isBadRequest()).andReturn();

        assertInstanceOf(MethodArgumentTypeMismatchException.class, result.getResolvedException());
        assertEquals("", result.getResponse().getContentAsString());
        get(BASE).andExpect(jsonPath("$.length()").value(5));
        assertEquals(5, ratingRepository.count());
    }

    // ---------------------------------------------------------------- [U05] [AC04]

    @Test
    void U05_AC04_TC01() throws Exception {
        List<Rating> five = seedFive();

        delete(BASE + "/" + five.get(2).getId()).andExpect(status().isOk());

        get(BASE).andExpect(jsonPath("$.length()").value(4));
        assertTrue(ratingRepository.findById(five.get(2).getId()).isEmpty());
        assertEquals(4, ratingRepository.count());
    }

    @Test
    void U05_AC04_TC02() throws Exception {
        seedFive();

        MvcResult result = delete(BASE).andExpect(status().isMethodNotAllowed()).andReturn();

        assertInstanceOf(HttpRequestMethodNotSupportedException.class, result.getResolvedException());
        assertEquals("", result.getResponse().getContentAsString());
        get(BASE).andExpect(jsonPath("$.length()").value(5));
        assertEquals(5, ratingRepository.count());
    }
}
