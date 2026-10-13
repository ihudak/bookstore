package com.dynatrace.ratings.controller;

import com.dynatrace.ratings.repository.RatingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Over a real HTTP port: the error bodies the advice renders carry "message"; Boot's own carry none.
// None of these cases reaches orders.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RatingErrorBodiesIT {
    private static final String BASE = "/api/v1/ratings";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearRatings() {
        ratingRepository.deleteAllInBatch();
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new TreeSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    @Test
    void getUnknownRatingAnswersBootBodyWithoutMessage() throws Exception {
        ResponseEntity<String> response = testRestTemplate.getForEntity(BASE + "/999999", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        JsonNode body = body(response);
        assertEquals(Set.of("error", "path", "status", "timestamp"), keys(body));
        assertEquals(404, body.get("status").asInt());
    }

    @Test
    void putUnknownRatingAnswersBootBodyWithoutMessage() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange(BASE + "/999999", HttpMethod.PUT,
                json("{\"id\":999999,\"rating\":4}"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Set.of("error", "path", "status", "timestamp"), keys(body(response)));
    }

    @Test
    void deleteAllIsNoLongerAnEndpointAndAnswersBootBodyWithoutMessage() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange(BASE + "/delete-all", HttpMethod.DELETE,
                null, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        JsonNode body = body(response);
        assertEquals(Set.of("error", "path", "status", "timestamp"), keys(body));
        assertEquals(400, body.get("status").asInt());
    }

    @Test
    void deleteOnTheCollectionAnswersBootBodyWithoutMessage() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange(BASE, HttpMethod.DELETE, null, String.class);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        JsonNode body = body(response);
        assertEquals(Set.of("error", "path", "status", "timestamp"), keys(body));
        assertEquals(405, body.get("status").asInt());
    }

    @Test
    void invalidEmailAnswersTheAdviceBodyWithMessage() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange(BASE, HttpMethod.POST,
                json("{\"email\":\"reader-at-example\",\"isbn\":\"9780000000017\",\"rating\":4}"), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        JsonNode body = body(response);
        assertEquals(Set.of("error", "message", "path", "status", "timestamp"), keys(body));
        assertEquals(400, body.get("status").asInt());
        assertEquals("Bad Request", body.get("error").asText());
        assertEquals("Invalid email address", body.get("message").asText());
        assertEquals(BASE, body.get("path").asText());
    }
}
