package com.dynatrace.ratings.model;

import com.fasterxml.jackson.databind.JsonNode;

// The body of a give or a change. Each JsonNode field takes whatever JSON value it carries,
// so RatingService can refuse a fault in it with its own reason.
public record RatingRequest(JsonNode id, JsonNode email, JsonNode isbn, JsonNode rating, String comment) {
}
