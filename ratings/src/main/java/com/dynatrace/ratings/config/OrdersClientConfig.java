package com.dynatrace.ratings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class OrdersClientConfig {
    // the purchase check waits on orders at most connect-timeout + read-timeout, with no retry
    @Bean
    public RestTemplate ordersRestTemplate(RestTemplateBuilder builder,
                                           @Value("${ratings.orders.connect-timeout}") Duration connectTimeout,
                                           @Value("${ratings.orders.read-timeout}") Duration readTimeout) {
        return builder.connectTimeout(connectTimeout).readTimeout(readTimeout).build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
