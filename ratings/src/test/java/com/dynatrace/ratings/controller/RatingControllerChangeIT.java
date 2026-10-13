package com.dynatrace.ratings.controller;

import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.ratings.model.Rating;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.ConnectException;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// [U02] Change my rating of a book, through MockMvc.
class RatingControllerChangeIT extends RatingControllerTestBase {
    private static final String READER = "reader@example.com";
    private static final String BOOK = "9780000000017";
    private static final String OTHER_BOOK = "9780000000024";
    private static final String STARS_TEXT = "Invalid rating. Must be a whole number from 1 to 5.";
    private static final String EMAIL_TEXT = "Invalid email address";
    private static final String ISBN_TEXT = "ISBN must be a 13-digits value";
    private static final String MOVE_TEXT = "A rating's e-mail and ISBN cannot be changed.";
    private static final String BAD_ID_TEXT = "bad rating id";
    private static final String BUYERS_ONLY_TEXT = "Only customers who have bought this book can rate it.";
    private static final long UNKNOWN_ID = 999999L;

    // ---- helpers -------------------------------------------------------------------------------------------

    private ResultActions put(long id, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/ratings/" + id)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions post(String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String notCompleted(String isbn) {
        return "[{\"id\":1,\"isbn\":\"" + isbn + "\",\"quantity\":1,\"completed\":false}]";
    }

    private void expectMessage(ResultActions result, int statusCode, String message) throws Exception {
        result.andExpect(status().is(statusCode)).andExpect(jsonPath("$.message").value(message));
    }

    // a 200 whose raw JSON holds no comment key and none of the given comment texts
    private void assertOkWithoutComment(ResultActions result, String... commentTexts) throws Exception {
        result.andExpect(status().isOk());
        String raw = result.andReturn().getResponse().getContentAsString();
        assertFalse(raw.contains("\"comment\""), raw);
        for (String text : commentTexts) {
            assertFalse(raw.contains(text), raw);
        }
    }

    private void assertNotFoundWithoutBody(ResultActions result) throws Exception {
        MvcResult mvcResult = result.andExpect(status().isNotFound()).andReturn();
        assertInstanceOf(ResourceNotFoundException.class, mvcResult.getResolvedException());
        assertEquals("", mvcResult.getResponse().getContentAsString());
    }

    private void assertStored(long id, String email, String isbn, int stars) {
        Rating rating = stored(id);
        assertEquals(email, rating.getEmail());
        assertEquals(isbn, rating.getIsbn());
        assertEquals(stars, rating.getRating());
    }

    private static String refusalLine(CapturedOutput output) {
        List<String> lines = output.getAll().lines().filter(line -> line.contains("Rating refused")).toList();
        assertEquals(1, lines.size(), lines.toString());
        return lines.get(0);
    }

    // ---- [AC01] A buyer's change replaces the stars --------------------------------------------------------

    @Test
    void U02_AC01_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        ordersBuyer(READER, BOOK);

        assertOkWithoutComment(put(rating.getId(), ("{\"id\":%d,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4}")
                .formatted(rating.getId(), READER, BOOK)), "Great plot");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/findByEmail").param("email", READER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(READER))
                .andExpect(jsonPath("$[0].isbn").value(BOOK))
                .andExpect(jsonPath("$[0].rating").value(4));
        assertEquals(1, ratingRepository.findByEmail(READER).size());
    }

    @Test
    void U02_AC01_TC02() throws Exception {
        Rating rating = seed("direct@example.com", OTHER_BOOK, 5, null);
        ordersBuyer("direct@example.com", OTHER_BOOK);

        put(rating.getId(), "{\"id\":%d,\"rating\":3}".formatted(rating.getId())).andExpect(status().isOk());

        assertStored(rating.getId(), "direct@example.com", OTHER_BOOK, 3);
        assertEquals(1, ratingRepository.findByEmail("direct@example.com").size());
    }

    @Test
    void U02_AC01_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 4, null);
        ordersBuyer(READER, BOOK);
        ordersBuyer(READER, BOOK);
        String json = "{\"id\":%d,\"rating\":4}".formatted(rating.getId());

        put(rating.getId(), json).andExpect(status().isOk());
        put(rating.getId(), json).andExpect(status().isOk());

        assertEquals(1, ratingRepository.findByEmail(READER).size());
        assertStored(rating.getId(), READER, BOOK, 4);
    }

    // ---- [AC02] A saved change is answered with the saved rating -------------------------------------------

    @Test
    void U02_AC02_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        ordersBuyer(READER, BOOK);

        ResultActions result = put(rating.getId(), "{\"id\":%d,\"rating\":4}".formatted(rating.getId()));

        assertOkWithoutComment(result, "Great plot");
        result.andExpect(jsonPath("$.id").value(rating.getId()))
                .andExpect(jsonPath("$.email").value(READER))
                .andExpect(jsonPath("$.isbn").value(BOOK))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void U02_AC02_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        ordersBuyer(READER, BOOK);

        ResultActions result = post("{\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":5}".formatted(READER, BOOK));

        assertOkWithoutComment(result, "Great plot");
        result.andExpect(jsonPath("$.id").value(rating.getId())).andExpect(jsonPath("$.rating").value(5));
    }

    // ---- [AC03] Rating a book again changes the existing rating -------------------------------------------

    @Test
    void U02_AC03_TC01() throws Exception {
        seed(READER, BOOK, 2, null);
        ordersBuyer(READER, BOOK);

        post("{\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":5}".formatted(READER, BOOK))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/findByEmail").param("email", READER))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].isbn").value(BOOK))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void U02_AC03_TC02() throws Exception {
        Rating rating = seed("lapsed@example.com", BOOK, 3, null);
        ordersAnswer("lapsed@example.com", notCompleted(BOOK));

        expectMessage(post("{\"email\":\"lapsed@example.com\",\"isbn\":\"%s\",\"rating\":5}".formatted(BOOK)),
                403, BUYERS_ONLY_TEXT);

        assertEquals(1, ratingRepository.findByEmail("lapsed@example.com").size());
        assertStored(rating.getId(), "lapsed@example.com", BOOK, 3);
    }

    @Test
    void U02_AC03_TC03() throws Exception {
        Rating first = seed(READER, BOOK, 4, null);
        ordersBuyer("second@example.com", BOOK);

        post("{\"email\":\"second@example.com\",\"isbn\":\"%s\",\"rating\":1}".formatted(BOOK))
                .andExpect(status().isOk());

        List<Rating> ofBook = ratingRepository.findByIsbn(BOOK);
        assertEquals(2, ofBook.size());
        assertEquals(1, ratingRepository.findByEmail("second@example.com").size());
        assertEquals(1, ratingRepository.findByEmail(READER).size());
        assertStored(first.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC03_TC04() throws Exception {
        seed(READER, BOOK, 2, null);
        ordersBuyer("Reader@Example.com", BOOK);

        post("{\"email\":\"Reader@Example.com\",\"isbn\":\"%s\",\"rating\":5}".formatted(BOOK))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/findByISBN").param("isbn", BOOK))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    // ---- [AC05] An empty comment on a change keeps the stored comment --------------------------------------

    @Test
    void U02_AC05_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        ordersBuyer(READER, BOOK);

        assertOkWithoutComment(put(rating.getId(),
                "{\"id\":%d,\"rating\":4,\"comment\":\"\"}".formatted(rating.getId())), "Great plot");

        assertEquals(4, stored(rating.getId()).getRating());
        assertEquals("Great plot", stored(rating.getId()).getComment());
    }

    @Test
    void U02_AC05_TC02() throws Exception {
        Rating rating = seed("direct@example.com", OTHER_BOOK, 5, "Slow start");
        ordersBuyer("direct@example.com", OTHER_BOOK);

        assertOkWithoutComment(put(rating.getId(),
                "{\"id\":%d,\"rating\":4}".formatted(rating.getId())), "Slow start");

        assertEquals(4, stored(rating.getId()).getRating());
        assertEquals("Slow start", stored(rating.getId()).getComment());
    }

    @Test
    void U02_AC05_TC03() throws Exception {
        Rating rating = seed("direct@example.com", OTHER_BOOK, 5, "Slow start");
        ordersBuyer("direct@example.com", OTHER_BOOK);

        assertOkWithoutComment(put(rating.getId(),
                "{\"id\":%d,\"rating\":3,\"comment\":\"   \"}".formatted(rating.getId())), "Slow start");

        assertEquals(3, stored(rating.getId()).getRating());
        assertEquals("Slow start", stored(rating.getId()).getComment());
    }

    @Test
    void U02_AC05_TC04() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        ordersBuyer(READER, BOOK);

        assertOkWithoutComment(post("{\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4}".formatted(READER, BOOK)),
                "Great plot");

        assertEquals(4, stored(rating.getId()).getRating());
        assertEquals("Great plot", stored(rating.getId()).getComment());
    }

    // ---- [AC06] A comment on a change replaces the stored comment ------------------------------------------

    @Test
    void U02_AC06_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 4, "Great plot");
        ordersBuyer(READER, BOOK);

        assertOkWithoutComment(put(rating.getId(),
                "{\"id\":%d,\"rating\":3,\"comment\":\"Weaker second half\"}".formatted(rating.getId())),
                "Weaker second half", "Great plot");

        assertEquals("Weaker second half", stored(rating.getId()).getComment());
    }

    @Test
    void U02_AC06_TC02() throws Exception {
        Rating rating = seed("lapsed@example.com", BOOK, 4, "Good read");
        ordersAnswer("lapsed@example.com", notCompleted(BOOK));

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"rating\":5,\"comment\":\"Changed my mind\"}".formatted(rating.getId())),
                403, BUYERS_ONLY_TEXT);

        assertEquals("Good read", stored(rating.getId()).getComment());
        assertEquals(4, stored(rating.getId()).getRating());
    }

    @Test
    void U02_AC06_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 4, "Great plot");
        ordersBuyer(READER, BOOK);

        assertOkWithoutComment(post(
                "{\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":3,\"comment\":\"Weaker second half\"}"
                        .formatted(READER, BOOK)), "Weaker second half", "Great plot");

        assertEquals("Weaker second half", stored(rating.getId()).getComment());
        assertEquals(3, stored(rating.getId()).getRating());
    }

    // ---- [AC08] A change keeps the stored e-mail as stored -------------------------------------------------

    @Test
    void U02_AC08_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer("Reader@Example.com", BOOK);

        put(rating.getId(), "{\"id\":%d,\"email\":\"Reader@Example.com\",\"rating\":4}".formatted(rating.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(READER));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/" + rating.getId()))
                .andExpect(jsonPath("$.email").value(READER))
                .andExpect(jsonPath("$.rating").value(4));
        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC08_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer("READER@EXAMPLE.COM", BOOK);

        post("{\"email\":\"READER@EXAMPLE.COM\",\"isbn\":\"%s\",\"rating\":5}".formatted(BOOK))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/" + rating.getId()))
                .andExpect(jsonPath("$.email").value(READER))
                .andExpect(jsonPath("$.rating").value(5));
        assertStored(rating.getId(), READER, BOOK, 5);
    }

    // ---- [AC09] Stars outside the scale are refused on a change --------------------------------------------

    @Test
    void U02_AC09_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 4, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":0}".formatted(rating.getId())), 400, STARS_TEXT);

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC09_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 4, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d}".formatted(rating.getId())), 400, STARS_TEXT);

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC09_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 4, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":6}".formatted(rating.getId())), 400, STARS_TEXT);
        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":2.5}".formatted(rating.getId())), 400, STARS_TEXT);

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC09_TC04() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":4.0}".formatted(rating.getId())), 400, STARS_TEXT);
        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":\"4\"}".formatted(rating.getId())), 400,
                STARS_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC10] A malformed e-mail or ISBN on a change is refused ------------------------------------------

    @Test
    void U02_AC10_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"email\":\"reader-at-example\",\"rating\":4}".formatted(rating.getId())),
                400, EMAIL_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void U02_AC10_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"isbn\":\"978000000001\",\"rating\":4}".formatted(rating.getId())),
                400, ISBN_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC11] A change cannot move a rating --------------------------------------------------------------

    @Test
    void U02_AC11_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"isbn\":\"%s\",\"rating\":3}".formatted(rating.getId(), OTHER_BOOK)),
                400, MOVE_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void U02_AC11_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"email\":\"other@example.com\",\"rating\":3}".formatted(rating.getId())),
                400, MOVE_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void U02_AC11_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer(READER, BOOK);
        ordersBuyer(READER, BOOK);

        put(rating.getId(), "{\"id\":%d,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":3}"
                .formatted(rating.getId(), READER, BOOK)).andExpect(status().isOk());
        put(rating.getId(), "{\"id\":%d,\"rating\":4}".formatted(rating.getId())).andExpect(status().isOk());

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    // ---- [AC12] A change to a rating that does not exist is refused ---------------------------------------

    @Test
    void U02_AC12_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 3, null);
        long id = rating.getId();
        ordersNeverCalled();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/ratings/" + id)).andExpect(status().isOk());
        assertNotFoundWithoutBody(put(id,
                "{\"id\":%d,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4}".formatted(id, READER, BOOK)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/findByEmail").param("email", READER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertTrue(ratingRepository.findByEmail(READER).isEmpty());
    }

    @Test
    void U02_AC12_TC02() throws Exception {
        ordersNeverCalled();

        assertNotFoundWithoutBody(put(UNKNOWN_ID,
                "{\"id\":999998,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4}".formatted(READER, BOOK)));

        assertEquals(0, ratingRepository.count());
    }

    @Test
    void U02_AC12_TC03() throws Exception {
        ordersNeverCalled();

        expectMessage(put(UNKNOWN_ID,
                "{\"id\":999999,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":0}".formatted(READER, BOOK)),
                400, STARS_TEXT);
        expectMessage(put(UNKNOWN_ID,
                "{\"id\":999999,\"email\":\"reader-at-example\",\"isbn\":\"%s\",\"rating\":4}".formatted(BOOK)),
                400, EMAIL_TEXT);

        assertEquals(0, ratingRepository.count());
    }

    // ---- [AC13] A change whose body id does not match its path is refused ----------------------------------

    @Test
    void U02_AC13_TC01() throws Exception {
        Rating first = seed(READER, BOOK, 2, null);
        Rating second = seed("second@example.com", BOOK, 3, null);
        ordersNeverCalled();

        expectMessage(put(first.getId(), "{\"id\":%d,\"rating\":5}".formatted(second.getId())), 400, BAD_ID_TEXT);

        assertStored(first.getId(), READER, BOOK, 2);
        assertStored(second.getId(), "second@example.com", BOOK, 3);
    }

    @Test
    void U02_AC13_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer(READER, BOOK);

        put(rating.getId(), "{\"id\":%d,\"rating\":5}".formatted(rating.getId())).andExpect(status().isOk());

        assertStored(rating.getId(), READER, BOOK, 5);
    }

    @Test
    void U02_AC13_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"rating\":5}"), 400, BAD_ID_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC14] A change with several faults is refused with one of their reasons --------------------------

    @Test
    void U02_AC14_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 4, null);
        ordersNeverCalled();

        put(rating.getId(), "{\"id\":%d,\"isbn\":\"%s\",\"rating\":0}".formatted(rating.getId(), OTHER_BOOK))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", anyOf(is(STARS_TEXT), is(MOVE_TEXT))));

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void U02_AC14_TC02() throws Exception {
        Rating first = seed(READER, BOOK, 2, null);
        Rating second = seed("second@example.com", BOOK, 3, null);
        ordersNeverCalled();

        put(first.getId(), "{\"id\":%d,\"rating\":6}".formatted(second.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", anyOf(is(BAD_ID_TEXT), is(STARS_TEXT))));

        assertStored(first.getId(), READER, BOOK, 2);
        assertStored(second.getId(), "second@example.com", BOOK, 3);
    }

    // ---- [AC15] An invalid change makes no purchase check --------------------------------------------------

    @Test
    void U02_AC15_TC01() throws Exception {
        Rating rating = seed("lapsed@example.com", BOOK, 3, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":0}".formatted(rating.getId())), 400, STARS_TEXT);

        assertStored(rating.getId(), "lapsed@example.com", BOOK, 3);
    }

    @Test
    void U02_AC15_TC02() throws Exception {
        ordersNeverCalled();

        assertNotFoundWithoutBody(put(UNKNOWN_ID,
                "{\"id\":999999,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4}".formatted(READER, BOOK)));
    }

    @Test
    void U02_AC15_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"isbn\":\"%s\",\"rating\":3}".formatted(rating.getId(), OTHER_BOOK)),
                400, MOVE_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC16] A change without a qualifying order is refused ---------------------------------------------

    @Test
    void U02_AC16_TC01(CapturedOutput output) throws Exception {
        Rating rating = seed("lapsed@example.com", BOOK, 3, "Good read");
        ordersAnswer("lapsed@example.com", notCompleted(BOOK));

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":5}".formatted(rating.getId())),
                403, BUYERS_ONLY_TEXT);

        assertStored(rating.getId(), "lapsed@example.com", BOOK, 3);
        String line = refusalLine(output);
        assertTrue(line.contains(BOOK), line);
        assertTrue(line.contains("ORDER_NOT_COMPLETED"), line);
        assertFalse(line.contains("@"), line);
    }

    @Test
    void U02_AC16_TC02(CapturedOutput output) throws Exception {
        Rating rating = seed("gone@example.com", BOOK, 4, null);
        ordersAnswer("gone@example.com", "[]");

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":1}".formatted(rating.getId())),
                403, BUYERS_ONLY_TEXT);

        assertStored(rating.getId(), "gone@example.com", BOOK, 4);
        String line = refusalLine(output);
        assertTrue(line.contains(BOOK), line);
        assertTrue(line.contains("NO_ORDER_FOR_BOOK"), line);
        assertFalse(line.contains("@"), line);
    }

    @Test
    void U02_AC16_TC03() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersAnswer("READER@EXAMPLE.COM", "[]");

        expectMessage(put(rating.getId(),
                "{\"id\":%d,\"email\":\"READER@EXAMPLE.COM\",\"rating\":4}".formatted(rating.getId())),
                403, BUYERS_ONLY_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC17] A change is refused while orders cannot answer ---------------------------------------------

    @Test
    void U02_AC17_TC01() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        orders.expect(ExpectedCount.once(), requestTo(findByEmail(READER)))
                .andRespond(withException(new ConnectException("refused")));

        put(rating.getId(), "{\"id\":%d,\"rating\":4}".formatted(rating.getId()))
                .andExpect(status().isServiceUnavailable());

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void U02_AC17_TC02() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        orders.expect(ExpectedCount.once(), requestTo(findByEmail(READER)))
                .andRespond(withException(new ConnectException("refused")));

        post("{\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":5,\"comment\":\"Even better\"}".formatted(READER, BOOK))
                .andExpect(status().isServiceUnavailable());

        assertStored(rating.getId(), READER, BOOK, 2);
        assertEquals("Great plot", stored(rating.getId()).getComment());
    }

    @Test
    void U02_AC17_TC04() throws Exception {
        Rating rating = seed("lapsed@example.com", BOOK, 3, null);
        orders.expect(ExpectedCount.once(), requestTo(findByEmail("lapsed@example.com")))
                .andRespond(withServerError());

        put(rating.getId(), "{\"id\":%d,\"rating\":5}".formatted(rating.getId()))
                .andExpect(status().isServiceUnavailable());

        assertStored(rating.getId(), "lapsed@example.com", BOOK, 3);
    }

    @Test
    void U02_AC17_TC05() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersAnswer(READER, "<html>not an order list</html>");

        put(rating.getId(), "{\"id\":%d,\"rating\":4}".formatted(rating.getId()))
                .andExpect(status().isServiceUnavailable());

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    // ---- [AC18] A rating outlives its order's cancellation -------------------------------------------------

    @Test
    void U02_AC18_TC01() throws Exception {
        seed("keeper@example.com", "9780000000031", 4, null);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/summary")
                .param("isbn", "9780000000031"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.average").value(4.0));
        assertTrue(result.andReturn().getResponse().getContentAsString().contains("\"average\":4.0"));
    }

    @Test
    void U02_AC18_TC02() throws Exception {
        seed("keeper2@example.com", "9780000000048", 5, null);
        seed("another@example.com", "9780000000048", 3, null);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/summary")
                .param("isbn", "9780000000048"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.average").value(4.0));
    }

    @Test
    void U02_AC18_TC03() throws Exception {
        Rating rating = seed("lapsed2@example.com", "9780000000055", 2, null);
        ordersAnswer("lapsed2@example.com", notCompleted("9780000000055"));

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":5}".formatted(rating.getId())),
                403, BUYERS_ONLY_TEXT);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ratings/summary").param("isbn", "9780000000055"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.average").value(2.0));
    }

    // ---- operator removal between a change's checks and its write -----------------------------------------

    @Test
    void changeOfRatingRemovedBetweenChecksAndWriteIsNotFoundAndSavesNothing() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Great plot");
        long id = rating.getId();
        ResponseCreator removeThenAnswerBuyer = request -> {
            ratingRepository.deleteById(id);
            return withSuccess("[{\"isbn\":\"" + BOOK + "\",\"completed\":true}]", MediaType.APPLICATION_JSON)
                    .createResponse(request);
        };
        orders.expect(ExpectedCount.once(), requestTo(findByEmail(READER))).andRespond(removeThenAnswerBuyer);

        assertNotFoundWithoutBody(put(id, "{\"id\":%d,\"rating\":4}".formatted(id)));

        assertTrue(ratingRepository.findById(id).isEmpty());
        assertEquals(0, ratingRepository.count());
    }

    // ---- Review focus: PUT ---------------------------------------------------------------------------------

    @Test
    void starsOutsideTheIntRangeAreRefusedNotWrappedIntoTheScale() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":%d,\"rating\":4294967300}".formatted(rating.getId())),
                400, STARS_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void bodyIdOutsideTheLongRangeIsBadRatingId() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":18446744073709551617,\"rating\":4}"), 400, BAD_ID_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void negativeBodyIdThatWrapsToNothingUsefulIsBadRatingId() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":-4294967291,\"rating\":4}"), 400, BAD_ID_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void nullEmailOnAChangeIsNotCarriedAndTheStoredEmailIsChecked() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer(READER, BOOK);

        put(rating.getId(), "{\"id\":%d,\"email\":null,\"rating\":4}".formatted(rating.getId()))
                .andExpect(status().isOk());

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void nullIsbnOnAChangeIsNotCarriedAndTheStoredIsbnIsChecked() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersBuyer(READER, BOOK);

        put(rating.getId(), "{\"id\":%d,\"isbn\":null,\"rating\":4}".formatted(rating.getId()))
                .andExpect(status().isOk());

        assertStored(rating.getId(), READER, BOOK, 4);
    }

    @Test
    void nullBodyIdOnAChangeIsBadRatingId() throws Exception {
        Rating rating = seed(READER, BOOK, 2, null);
        ordersNeverCalled();

        expectMessage(put(rating.getId(), "{\"id\":null,\"rating\":4}"), 400, BAD_ID_TEXT);

        assertStored(rating.getId(), READER, BOOK, 2);
    }

    @Test
    void storefrontShapedChangeBodyIsAcceptedAndKeepsCreatedAt() throws Exception {
        Rating rating = seed(READER, BOOK, 2, "Old words");
        long id = rating.getId();
        java.time.Instant createdBefore = stored(id).getCreatedAt().toInstant();
        ordersBuyer(READER, BOOK);

        String json = ("{\"id\":%d,\"email\":\"%s\",\"isbn\":\"%s\",\"rating\":4,\"comment\":\"New words\","
                + "\"createdAt\":\"2020-01-01T00:00:00.000+00:00\",\"updatedAt\":\"2020-01-01T00:00:00.000+00:00\"}")
                .formatted(id, READER, BOOK);
        assertOkWithoutComment(put(id, json), "New words", "Old words");

        Rating after = stored(id);
        assertEquals(4, after.getRating());
        assertEquals("New words", after.getComment());
        assertEquals(createdBefore, after.getCreatedAt().toInstant());
    }
}
