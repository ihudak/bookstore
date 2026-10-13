package com.dynatrace.ratings.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// A clock the test sets, so Created At and Updated At can be compared exactly.
public class SteppedClock extends Clock {
    private volatile Instant now = Instant.parse("2026-10-13T10:00:00Z");

    public void set(Instant instant) {
        this.now = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
