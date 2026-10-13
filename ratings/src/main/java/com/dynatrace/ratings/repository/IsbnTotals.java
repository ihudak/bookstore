package com.dynatrace.ratings.repository;

// the summary queries' projection; count() and sum(int) are typed as Long
public record IsbnTotals(String isbn, Long count, Long sum) {
}
