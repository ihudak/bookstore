package com.dynatrace.ratings.controller;

import com.dynatrace.ratings.model.Rating;
import com.dynatrace.ratings.repository.RatingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// The whole HTTP surface through MockMvc: the real RatingService, the H2 database and the real orders
// adapter, whose RestTemplate answers from a MockRestServiceServer instead of the orders service.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
abstract class RatingControllerTestBase {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected RatingRepository ratingRepository;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    @Qualifier("ordersRestTemplate")
    protected RestTemplate ordersRestTemplate;
    @Value("${http.service.orders}")
    protected String ordersUrl;

    protected MockRestServiceServer orders;

    @BeforeEach
    void resetRatingsAndOrders() {
        ratingRepository.deleteAllInBatch();
        orders = MockRestServiceServer.bindTo(ordersRestTemplate).ignoreExpectOrder(true).build();
    }

    @AfterEach
    void verifyOrdersCalls() {
        orders.verify();
    }

    // the URI the adapter calls for an e-mail
    protected URI findByEmail(String email) {
        return UriComponentsBuilder.fromUriString(ordersUrl)
                .path("/findByEmail")
                .queryParam("email", "{email}")
                .encode()
                .buildAndExpand(email)
                .toUri();
    }

    // orders answers 200 with this JSON array of orders, e.g. [{"email":"…","isbn":"…","completed":true}]
    protected ResponseActions ordersAnswer(String email, String ordersJson) {
        ResponseActions actions = orders.expect(ExpectedCount.once(), requestTo(findByEmail(email)))
                .andExpect(method(HttpMethod.GET));
        actions.andRespond(withSuccess(ordersJson, MediaType.APPLICATION_JSON));
        return actions;
    }

    // a buyer: one completed order for the ISBN
    protected void ordersBuyer(String email, String isbn) {
        ordersAnswer(email, "[{\"id\":1,\"email\":\"" + email + "\",\"isbn\":\"" + isbn
                + "\",\"quantity\":1,\"price\":10.0,\"completed\":true}]");
    }

    // orders answers that the e-mail is not a customer
    protected void ordersNotACustomer(String email) {
        orders.expect(ExpectedCount.once(), requestTo(findByEmail(email)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/v1/orders/findByEmail\"}"));
    }

    // a write that must make no call to orders
    protected void ordersNeverCalled() {
        orders.expect(ExpectedCount.never(), requestTo(org.hamcrest.Matchers.startsWith(ordersUrl)));
    }

    protected Rating seed(String email, String isbn, int stars, String comment) {
        return ratingRepository.save(new Rating(email, isbn, stars, comment, Date.from(Instant.now())));
    }

    protected Rating stored(long id) {
        return ratingRepository.findById(id).orElseThrow();
    }
}
