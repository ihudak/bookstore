package com.dynatrace.ratings.service;

import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.model.RatingRequest;
import com.dynatrace.ratings.purchase.InMemoryPurchaseCheck;
import com.dynatrace.ratings.repository.RatingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
class RatingServiceConcurrencyTest {
    private static final String ISBN = "9780000000017";

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
        steppedClock.set(Instant.parse("2026-10-13T10:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        ratingRepository.deleteAllInBatch();
        inMemoryPurchaseCheck.reset();
    }

    private RatingRequest give(String email, int stars) {
        try {
            return objectMapper.readValue("{\"email\":\"" + email + "\",\"isbn\":\"" + ISBN
                    + "\",\"rating\":" + stars + ",\"comment\":\"c" + stars + "\"}", RatingRequest.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // releases every give at the same moment; each future must complete without an exception
    private List<Rating> giveTogether(List<RatingRequest> requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            CountDownLatch ready = new CountDownLatch(requests.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Rating>> futures = new ArrayList<>();
            for (RatingRequest request : requests) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return ratingService.give(request);
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "every give is waiting at the gate");
            go.countDown();
            List<Rating> answers = new ArrayList<>();
            for (Future<Rating> future : futures) {
                answers.add(future.get(30, TimeUnit.SECONDS));
            }
            return answers;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void U02_AC04_TC01() throws Exception {
        inMemoryPurchaseCheck.order("racer@example.com", ISBN, true);

        List<Rating> answers = giveTogether(List.of(give("racer@example.com", 3), give("racer@example.com", 5)));

        assertEquals(2, answers.size());
        assertEquals(answers.get(0).getId(), answers.get(1).getId());
        List<Rating> rows = ratingRepository.findByIsbn(ISBN);
        assertEquals(1, rows.size());
        assertEquals("racer@example.com", rows.get(0).getEmail());
        assertTrue(rows.get(0).getRating() == 3 || rows.get(0).getRating() == 5);
    }

    @Test
    void U02_AC04_TC02() throws Exception {
        inMemoryPurchaseCheck.order("racer1@example.com", ISBN, true);
        inMemoryPurchaseCheck.order("racer2@example.com", ISBN, true);

        giveTogether(List.of(give("racer1@example.com", 4), give("racer2@example.com", 4)));

        assertEquals(2, ratingRepository.findByIsbn(ISBN).size());
        assertEquals(1, ratingRepository.findByEmail("racer1@example.com").size());
        assertEquals(1, ratingRepository.findByEmail("racer2@example.com").size());
    }

    @Test
    void U02_AC04_TC03() throws Exception {
        inMemoryPurchaseCheck.order("racer3@example.com", ISBN, true);
        inMemoryPurchaseCheck.order("RACER3@example.com", ISBN, true);

        giveTogether(List.of(give("racer3@example.com", 2), give("RACER3@example.com", 4)));

        List<Rating> rows = ratingRepository.findByIsbn(ISBN);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getRating() == 2 || rows.get(0).getRating() == 4);
    }

    @Test
    void U02_AC04_TC04() throws Exception {
        inMemoryPurchaseCheck.order("burst@example.com", ISBN, true);
        List<RatingRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requests.add(give("burst@example.com", i % 5 + 1));
        }

        List<Rating> answers = giveTogether(requests);

        assertEquals(10, answers.size());
        List<Rating> rows = ratingRepository.findByEmail("burst@example.com");
        assertEquals(1, rows.size());
        assertEquals(1, ratingRepository.findByIsbn(ISBN).size());
        for (Rating answer : answers) {
            assertEquals(rows.get(0).getId(), answer.getId());
        }
    }
}
