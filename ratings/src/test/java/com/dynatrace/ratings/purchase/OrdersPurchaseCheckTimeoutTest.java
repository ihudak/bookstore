package com.dynatrace.ratings.purchase;

import com.dynatrace.ratings.config.OrdersClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Real sockets: a loopback server that accepts the connection and never answers.
// The bound is 1 s to connect + 3 s to read, with no retry.
class OrdersPurchaseCheckTimeoutTest {
    private static final String EMAIL = "reader@example.com";
    private static final String ISBN = "9780000000017";

    private ServerSocket serverSocket;
    private final List<Socket> accepted = new CopyOnWriteArrayList<>();
    private OrdersPurchaseCheck check;

    @BeforeEach
    void setUp() throws IOException {
        serverSocket = new ServerSocket(0);
        Thread acceptor = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    accepted.add(serverSocket.accept());
                }
            } catch (IOException ignored) {
                // the socket was closed by the test
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();

        RestTemplate restTemplate = new OrdersClientConfig()
                .ordersRestTemplate(new RestTemplateBuilder(), Duration.ofSeconds(1), Duration.ofSeconds(3));
        check = new OrdersPurchaseCheck(restTemplate,
                "http://127.0.0.1:" + serverSocket.getLocalPort() + "/api/v1/orders");
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close();
        for (Socket socket : accepted) {
            socket.close();
        }
    }

    private void assertUnavailableWithinBoundedTime() {
        long start = System.nanoTime();
        assertThrows(OrdersUnavailableException.class, () -> check.check(EMAIL, ISBN));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertTrue(elapsedMillis >= 2500, "gave up before the read bound: " + elapsedMillis + " ms");
        assertTrue(elapsedMillis < 4500, "waited past the bounded time: " + elapsedMillis + " ms");
    }

    @Test
    void U01_AC09_TC03() {
        assertUnavailableWithinBoundedTime();
    }

    @Test
    void U02_AC17_TC03() {
        assertUnavailableWithinBoundedTime();
    }
}
