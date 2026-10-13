package com.dynatrace.ratings.purchase;

public interface PurchaseCheck {
    /** Read with no side effect. Throws OrdersUnavailableException for every outcome that is not an answer about the e-mail. */
    Eligibility check(String email, String isbn);
}
