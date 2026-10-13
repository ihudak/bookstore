package com.dynatrace.ratings.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrdersPurchaseCheckTest {
    private static final String ORDERS_URL = "http://orders.test/api/v1/orders";
    private static final String EMAIL = "reader@example.com";
    private static final String ISBN = "9780000000017";
    private static final String OTHER_ISBN = "9780000000024";
    private static final String FIND = ORDERS_URL + "/findByEmail?email=reader%40example.com";

    private MockRestServiceServer server;
    private OrdersPurchaseCheck check;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        check = new OrdersPurchaseCheck(restTemplate, ORDERS_URL);
    }

    private void expectJson(String body) {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectStatus(HttpStatus status) {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(status));
    }

    private static String line(String isbn, boolean completed) {
        return "{\"isbn\":\"" + isbn + "\",\"completed\":" + completed + "}";
    }

    @Test
    void completedOrderForTheBookIsEligible() {
        expectJson("[" + line(ISBN, true) + "]");
        assertEquals(Eligibility.ELIGIBLE, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void notCompletedOrderForTheBookIsOrderNotCompleted() {
        expectJson("[" + line(ISBN, false) + "]");
        assertEquals(Eligibility.ORDER_NOT_COMPLETED, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void ordersOfAnotherBookOnlyIsNoOrderForBook() {
        expectJson("[" + line(OTHER_ISBN, true) + "]");
        assertEquals(Eligibility.NO_ORDER_FOR_BOOK, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void emptyOrderListIsNoOrderForBook() {
        expectJson("[]");
        assertEquals(Eligibility.NO_ORDER_FOR_BOOK, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void completedOrderOfAnotherBookDoesNotMakeThisBookEligible() {
        expectJson("[" + line(OTHER_ISBN, true) + "," + line(ISBN, false) + "]");
        assertEquals(Eligibility.ORDER_NOT_COMPLETED, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void completedOrderAfterAnOpenOneForTheSameBookIsEligible() {
        expectJson("[" + line(ISBN, false) + "," + line(ISBN, true) + "]");
        assertEquals(Eligibility.ELIGIBLE, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void notFoundWithJsonBodyIsNotACustomer() {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":404,\"error\":\"Not Found\"}"));
        assertEquals(Eligibility.NOT_A_CUSTOMER, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void notFoundWithEmptyBodyIsNotACustomer() {
        expectStatus(HttpStatus.NOT_FOUND);
        assertEquals(Eligibility.NOT_A_CUSTOMER, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void notFoundWithHtmlBodyIsNotACustomer() {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                        .body("<html><body>Not Found</body></html>"));
        assertEquals(Eligibility.NOT_A_CUSTOMER, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void badRequestIsUnavailable() {
        expectStatus(HttpStatus.BAD_REQUEST);
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void serverErrorIsUnavailable() {
        expectStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void serviceUnavailableIsUnavailable() {
        expectStatus(HttpStatus.SERVICE_UNAVAILABLE);
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void noContentIsUnavailable() {
        expectStatus(HttpStatus.NO_CONTENT);
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void htmlSuccessBodyIsUnavailable() {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andRespond(withSuccess("<html><body>login</body></html>", MediaType.TEXT_HTML));
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void jsonObjectInsteadOfArrayIsUnavailable() {
        expectJson("{}");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void nullElementIsUnavailable() {
        expectJson("[null]");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void elementLackingCompletedIsUnavailable() {
        expectJson("[{\"isbn\":\"" + ISBN + "\"}]");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void elementLackingIsbnIsUnavailable() {
        expectJson("[{\"completed\":true}]");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void malformedElementAfterAQualifyingOneIsStillUnavailable() {
        expectJson("[" + line(ISBN, true) + ",{\"isbn\":\"" + OTHER_ISBN + "\"}]");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void emptySuccessBodyIsUnavailable() {
        expectJson("");
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void unknownOrderFieldsAreIgnored() {
        expectJson("[{\"id\":7,\"email\":\"" + EMAIL + "\",\"isbn\":\"" + ISBN
                + "\",\"quantity\":2,\"price\":12.5,\"completed\":true,\"extra\":{\"a\":1}}]");
        assertEquals(Eligibility.ELIGIBLE, check.check(EMAIL, ISBN));
        server.verify();
    }

    @Test
    void emailIsEncodedInTheQuery() {
        server.expect(ExpectedCount.once(),
                        requestTo(URI.create("http://orders.test/api/v1/orders/findByEmail?email=reader%2Btag%40example.com")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[" + line(ISBN, true) + "]", MediaType.APPLICATION_JSON));
        assertEquals(Eligibility.ELIGIBLE, check.check("reader+tag@example.com", ISBN));
        server.verify();
    }

    @Test
    void timeoutIsUnavailableAndItsCauseChainCarriesNoEmail() {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        OrdersUnavailableException ex = assertThrows(OrdersUnavailableException.class,
                () -> check.check(EMAIL, ISBN));
        assertNoEmailInChain(ex);
        server.verify();
    }

    @Test
    void refusedConnectionIsUnavailableAndItsCauseChainCarriesNoEmail() {
        server.expect(ExpectedCount.once(), requestTo(FIND))
                .andRespond(withException(new ConnectException("Connection refused")));
        OrdersUnavailableException ex = assertThrows(OrdersUnavailableException.class,
                () -> check.check(EMAIL, ISBN));
        assertNoEmailInChain(ex);
        server.verify();
    }

    private static void assertNoEmailInChain(Throwable top) {
        for (Throwable t = top; t != null; t = t.getCause()) {
            String message = String.valueOf(t.getMessage());
            assertFalse(message.contains("@"), "message carries the e-mail: " + t.getClass());
            assertFalse(message.contains("%40"), "message carries the encoded e-mail: " + t.getClass());
            if (t.getCause() == t) {
                break;
            }
        }
    }
}
