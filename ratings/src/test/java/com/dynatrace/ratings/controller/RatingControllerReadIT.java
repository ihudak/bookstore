package com.dynatrace.ratings.controller;

import com.dynatrace.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// [U03] the summaries and [U04] the comment that no answer returns, at HTTP level.
class RatingControllerReadIT extends RatingControllerTestBase {

    // ---- helpers ----

    private String getRaw(String url, String... params) throws Exception {
        var request = get(url);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String summaryRaw(String isbn) throws Exception {
        return getRaw("/api/v1/ratings/summary", "isbn", isbn);
    }

    // one rating per star value, each from its own rater
    private void seedStars(String isbn, String prefix, int... stars) {
        for (int i = 0; i < stars.length; i++) {
            seed(prefix + i + "@example.com", isbn, stars[i], null);
        }
    }

    private void seedRepeated(String isbn, String prefix, int from, int count, int stars) {
        for (int i = from; i < from + count; i++) {
            seed(prefix + i + "@example.com", isbn, stars, null);
        }
    }

    private String body(String email, String isbn, int stars, String commentJson) {
        return "{\"email\":\"" + email + "\",\"isbn\":\"" + isbn + "\",\"rating\":" + stars
                + (commentJson == null ? "" : ",\"comment\":" + commentJson) + "}";
    }

    private String give(String email, String isbn, int stars, String commentJson) throws Exception {
        return mockMvc.perform(post("/api/v1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, isbn, stars, commentJson)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void assertNoComment(String raw, String commentText) {
        assertFalse(raw.contains("\"comment\""), raw);
        assertFalse(raw.contains(commentText), raw);
    }

    // ---- [U03] [AC01] ----

    @Test
    void U03_AC01_TC01() throws Exception {
        seedStars("9780000000062", "r", 5, 4, 4, 4);

        String raw = summaryRaw("9780000000062");

        assertTrue(raw.contains("\"average\":4.3"), raw);
        assertEquals("9780000000062", objectMapper.readTree(raw).get("isbn").asText());
        assertEquals(4, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC01_TC02() throws Exception {
        String isbn = "9780000000086";
        seedRepeated(isbn, "low", 0, 19, 1);
        seed("high@example.com", isbn, 2, null);

        String raw = summaryRaw(isbn);

        assertTrue(raw.contains("\"average\":1.1"), raw);
        assertEquals(20, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC01_TC03() throws Exception {
        seedStars("9780000000079", "even", 4, 4, 4);
        seedStars("9780000000192", "third", 4, 5, 5);

        String whole = summaryRaw("9780000000079");
        String repeating = summaryRaw("9780000000192");

        assertTrue(whole.contains("\"average\":4.0"), whole);
        assertEquals(3, objectMapper.readTree(whole).get("count").asInt());
        assertTrue(repeating.contains("\"average\":4.7"), repeating);
        assertEquals(3, objectMapper.readTree(repeating).get("count").asInt());
    }

    // ---- [U03] [AC02] ----

    @Test
    void U03_AC02_TC01() throws Exception {
        seedStars("9780000000062", "other", 5);

        String raw = summaryRaw("9780000000123");

        assertTrue(raw.contains("\"average\":null"), raw);
        assertEquals("9780000000123", objectMapper.readTree(raw).get("isbn").asText());
        assertEquals(0, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC02_TC02() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/summary").param("isbn", "9789999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9789999999999"))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.average").value((Object) null));
    }

    @Test
    void U03_AC02_TC03() throws Exception {
        String isbn = "9780000000130";
        long id = seed("only@example.com", isbn, 3, null).getId();

        mockMvc.perform(delete("/api/v1/ratings/" + id)).andExpect(status().isOk());
        String raw = summaryRaw(isbn);

        assertTrue(raw.contains("\"average\":null"), raw);
        assertEquals(0, objectMapper.readTree(raw).get("count").asInt());
    }

    // ---- [U03] [AC03] ----

    @Test
    void U03_AC03_TC01() throws Exception {
        seedStars("9780000000062", "a", 5, 4, 4, 4);
        seed("b@example.com", "9780000000109", 5, null);

        String raw = getRaw("/api/v1/ratings/summaries");

        JsonNode list = objectMapper.readTree(raw);
        assertEquals(2, list.size(), raw);
        assertEquals("9780000000062", list.get(0).get("isbn").asText());
        assertEquals(4, list.get(0).get("count").asInt());
        assertEquals("9780000000109", list.get(1).get("isbn").asText());
        assertEquals(1, list.get(1).get("count").asInt());
        assertTrue(raw.contains("\"average\":4.3"), raw);
        assertTrue(raw.contains("\"average\":5.0"), raw);
        assertFalse(raw.contains("9780000000123"), raw);
    }

    @Test
    void U03_AC03_TC02() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void U03_AC03_TC03() throws Exception {
        String isbn = "9780000000086";
        seedRepeated(isbn, "low", 0, 19, 1);
        seed("high@example.com", isbn, 2, null);

        String all = getRaw("/api/v1/ratings/summaries");
        String own = summaryRaw(isbn);

        JsonNode list = objectMapper.readTree(all);
        assertEquals(1, list.size(), all);
        assertEquals(objectMapper.readTree(own), list.get(0));
        assertTrue(all.contains("\"average\":1.1"), all);
        assertEquals(20, list.get(0).get("count").asInt());
    }

    // ---- [U03] [AC04] ----

    @Test
    void U03_AC04_TC01() throws Exception {
        String isbn = "9780000000147";
        seed("first@example.com", isbn, 4, null);
        ordersBuyer("newbuyer@example.com", isbn);

        String before = summaryRaw(isbn);
        give("newbuyer@example.com", isbn, 2, null);
        String after = summaryRaw(isbn);

        assertTrue(before.contains("\"average\":4.0"), before);
        assertEquals(1, objectMapper.readTree(before).get("count").asInt());
        assertTrue(after.contains("\"average\":3.0"), after);
        assertEquals(2, objectMapper.readTree(after).get("count").asInt());
    }

    @Test
    void U03_AC04_TC02() throws Exception {
        String isbn = "9780000000154";
        long id = seed("reader8@example.com", isbn, 2, null).getId();
        ordersBuyer("reader8@example.com", isbn);

        mockMvc.perform(put("/api/v1/ratings/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"rating\":5}"))
                .andExpect(status().isOk());
        String raw = summaryRaw(isbn);

        assertTrue(raw.contains("\"average\":5.0"), raw);
        assertEquals(1, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC04_TC03() throws Exception {
        String isbn = "9780000000161";
        seed("five@example.com", isbn, 5, null);
        long oneStar = seed("one@example.com", isbn, 1, null).getId();

        mockMvc.perform(delete("/api/v1/ratings/" + oneStar)).andExpect(status().isOk());
        String raw = summaryRaw(isbn);

        assertTrue(raw.contains("\"average\":5.0"), raw);
        assertEquals(1, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC04_TC04() throws Exception {
        String isbn = "9780000000178";
        seed("holder@example.com", isbn, 4, null);
        ordersNotACustomer("browser@example.com");

        mockMvc.perform(post("/api/v1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content(body("browser@example.com", isbn, 1, null)))
                .andExpect(status().isForbidden());
        String raw = summaryRaw(isbn);

        assertTrue(raw.contains("\"average\":4.0"), raw);
        assertEquals(1, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC04_TC05() throws Exception {
        String isbn = "9780000000123";
        ordersBuyer("firstbuyer@example.com", isbn);

        give("firstbuyer@example.com", isbn, 4, null);
        String raw = getRaw("/api/v1/ratings/summaries");

        JsonNode list = objectMapper.readTree(raw);
        assertEquals(1, list.size(), raw);
        assertEquals(isbn, list.get(0).get("isbn").asText());
        assertEquals(1, list.get(0).get("count").asInt());
        assertTrue(raw.contains("\"average\":4.0"), raw);
    }

    // ---- [U03] [AC05] ----

    @Test
    void U03_AC05_TC01() throws Exception {
        ordersNeverCalled();

        MvcResult result = mockMvc.perform(get("/api/v1/ratings/summary"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertInstanceOf(MissingServletRequestParameterException.class, result.getResolvedException());
        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    void U03_AC05_TC02_empty() throws Exception {
        seed("someone@example.com", "9780000000123", 4, null);

        mockMvc.perform(get("/api/v1/ratings/summary").param("isbn", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("isbn is required"));
    }

    @Test
    void U03_AC05_TC02_spaces() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/summary").param("isbn", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("isbn is required"));
    }

    @Test
    void U03_AC05_notThirteenDigits_answersCountZero() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/summary").param("isbn", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("12345"))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.average").value((Object) null));
    }

    // ---- [U03] [AC06] ----

    @Test
    void U03_AC06_TC01() throws Exception {
        String isbn = "9780000000116";
        seed("reader@example.com", isbn, 5, "Secret note one");
        seed("second@example.com", isbn, 4, "Secret note two");

        String raw = summaryRaw(isbn);

        assertFalse(raw.contains("@"), raw);
        assertFalse(raw.contains("Secret note"), raw);
        assertEquals(Set.of("isbn", "count", "average"), fieldNames(objectMapper.readTree(raw)));
        assertEquals(2, objectMapper.readTree(raw).get("count").asInt());
    }

    @Test
    void U03_AC06_TC02() throws Exception {
        String isbn = "9780000000109";
        seed("solo@example.com", isbn, 5, "Solo remark");

        String raw = summaryRaw(isbn);

        assertFalse(raw.contains("solo@example.com"), raw);
        assertFalse(raw.contains("@"), raw);
        assertFalse(raw.contains("Solo remark"), raw);
        assertEquals(Set.of("isbn", "count", "average"), fieldNames(objectMapper.readTree(raw)));
    }

    @Test
    void U03_AC06_TC03() throws Exception {
        seed("one@example.com", "9780000000062", 5, "Remark A");
        seed("two@example.com", "9780000000109", 4, "Remark B");
        seed("three@example.com", "9780000000116", 3, "Remark C");

        String raw = getRaw("/api/v1/ratings/summaries");

        assertFalse(raw.contains("@"), raw);
        assertFalse(raw.contains("Remark"), raw);
        JsonNode list = objectMapper.readTree(raw);
        assertEquals(3, list.size(), raw);
        for (JsonNode entry : list) {
            assertEquals(Set.of("isbn", "count", "average"), fieldNames(entry));
        }
    }

    private Set<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return Set.copyOf(names);
    }

    // ---- [U04] [AC01] ----

    @Test
    void U04_AC01_TC01() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("commenter2@example.com", isbn);

        String answer = give("commenter2@example.com", isbn, 4, "\"Loved the ending\"");

        long id = objectMapper.readTree(answer).get("id").asLong();
        assertEquals("Loved the ending", stored(id).getComment());
    }

    @Test
    void U04_AC01_TC02() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("commenter3@example.com", isbn);

        String answer = give("commenter3@example.com", isbn, 4, "\"  Loved it\"");

        long id = objectMapper.readTree(answer).get("id").asLong();
        assertEquals("  Loved it", stored(id).getComment());
    }

    // ---- [U04] [AC02] ----

    @Test
    void U04_AC02_TC01() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("quiet@example.com", isbn);

        String answer = give("quiet@example.com", isbn, 3, null);

        long id = objectMapper.readTree(answer).get("id").asLong();
        assertEquals(3, stored(id).getRating());
        assertNull(stored(id).getComment());
    }

    @Test
    void U04_AC02_TC02() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("quiet2@example.com", isbn);

        String answer = give("quiet2@example.com", isbn, 3, "\"   \"");

        assertNull(stored(objectMapper.readTree(answer).get("id").asLong()).getComment());
    }

    @Test
    void U04_AC02_TC03() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("quiet3@example.com", isbn);

        String answer = give("quiet3@example.com", isbn, 3, "\"\"");

        assertNull(stored(objectMapper.readTree(answer).get("id").asLong()).getComment());
    }

    @Test
    void U04_AC02_newGiveWithOnlyTabsAndNewlines_storesNoComment() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("quiet4@example.com", isbn);

        String answer = give("quiet4@example.com", isbn, 3, "\"\\t\\n\\t\"");

        assertNull(stored(objectMapper.readTree(answer).get("id").asLong()).getComment());
    }

    // ---- [U04] [AC03] ----

    @Test
    void U04_AC03_TC01() throws Exception {
        String isbn = "9780000000017";
        long id = seed("reader@example.com", isbn, 4, "Great plot").getId();

        assertNoComment(getRaw("/api/v1/ratings/" + id), "Great plot");
        assertNoComment(getRaw("/api/v1/ratings/findByEmail", "email", "reader@example.com"), "Great plot");
        assertNoComment(getRaw("/api/v1/ratings/findByISBN", "isbn", isbn), "Great plot");
        assertNoComment(getRaw("/api/v1/ratings"), "Great plot");
        assertNoComment(summaryRaw(isbn), "Great plot");
        assertNoComment(getRaw("/api/v1/ratings/summaries"), "Great plot");
        assertEquals("Great plot", stored(id).getComment());
    }

    @Test
    void U04_AC03_TC02() throws Exception {
        String isbn = "9780000000017";
        ordersBuyer("commenter@example.com", isbn);

        String answer = give("commenter@example.com", isbn, 4, "\"Loved the ending\"");

        assertNoComment(answer, "Loved the ending");
        long id = objectMapper.readTree(answer).get("id").asLong();
        assertEquals("Loved the ending", stored(id).getComment());
    }

    @Test
    void U04_AC03_TC03() throws Exception {
        String isbn = "9780000000017";
        long id = seed("reader@example.com", isbn, 4, "Great plot").getId();
        ordersBuyer("reader@example.com", isbn);

        String answer = mockMvc.perform(put("/api/v1/ratings/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"rating\":5}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertNoComment(answer, "Great plot");
        assertEquals(5, objectMapper.readTree(answer).get("rating").asInt());
    }

    // ---- [U04] [AC04] ----

    @Test
    void U04_AC04_TC01() throws Exception {
        String isbn = "9780000000017";
        long id = seed("reader@example.com", isbn, 4, "Great plot").getId();

        String raw = getRaw("/api/v1/ratings/" + id);

        JsonNode node = objectMapper.readTree(raw);
        assertEquals(id, node.get("id").asLong());
        assertEquals("reader@example.com", node.get("email").asText());
        assertEquals(isbn, node.get("isbn").asText());
        assertEquals(4, node.get("rating").asInt());
        assertTrue(node.hasNonNull("updatedAt"), raw);
        assertTrue(node.hasNonNull("createdAt"), raw);

        // the list reads keep the same fields
        for (String list : new String[]{
                getRaw("/api/v1/ratings"),
                getRaw("/api/v1/ratings/findByEmail", "email", "reader@example.com"),
                getRaw("/api/v1/ratings/findByISBN", "isbn", isbn)}) {
            JsonNode first = objectMapper.readTree(list).get(0);
            assertEquals(id, first.get("id").asLong(), list);
            assertEquals("reader@example.com", first.get("email").asText(), list);
            assertEquals(isbn, first.get("isbn").asText(), list);
            assertEquals(4, first.get("rating").asInt(), list);
            assertTrue(first.hasNonNull("updatedAt"), list);
            assertTrue(first.hasNonNull("createdAt"), list);
        }
    }

    @Test
    void U04_AC04_TC02() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/ratings/999999"))
                .andExpect(status().isNotFound())
                .andReturn();

        assertInstanceOf(ResourceNotFoundException.class, result.getResolvedException());
        assertEquals("", result.getResponse().getContentAsString());
    }
}
