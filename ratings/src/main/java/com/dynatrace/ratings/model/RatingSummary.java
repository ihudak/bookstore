package com.dynatrace.ratings.model;

import java.math.BigDecimal;

// A book's rating count and average; average is null when count is 0.
public record RatingSummary(String isbn, long count, BigDecimal average) {
}
