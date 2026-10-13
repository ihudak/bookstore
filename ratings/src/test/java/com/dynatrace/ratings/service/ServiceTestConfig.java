package com.dynatrace.ratings.service;

import com.dynatrace.ratings.purchase.InMemoryPurchaseCheck;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// RatingService's tests run on the H2 database with the in-memory purchase check and a stepped clock.
// The bean methods are not named "clock": bean overriding is disabled.
@TestConfiguration(proxyBeanMethods = false)
public class ServiceTestConfig {
    @Bean
    @Primary
    public InMemoryPurchaseCheck inMemoryPurchaseCheck() {
        return new InMemoryPurchaseCheck();
    }

    @Bean
    @Primary
    public SteppedClock steppedClock() {
        return new SteppedClock();
    }
}
