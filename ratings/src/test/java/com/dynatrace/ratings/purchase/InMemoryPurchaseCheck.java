package com.dynatrace.ratings.purchase;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The in-memory adapter of PurchaseCheck: orders as a table of customers and their orders.
// E-mails match exactly, as orders matches them.
public class InMemoryPurchaseCheck implements PurchaseCheck {
    private final Set<String> customers = new HashSet<>();
    private final Map<String, Map<String, Boolean>> orders = new HashMap<>();
    private final List<String> askedEmails = new ArrayList<>();
    private boolean unavailable;
    private Runnable hook;

    public synchronized InMemoryPurchaseCheck customer(String email) {
        customers.add(email);
        return this;
    }

    // an order implies its customer
    public synchronized InMemoryPurchaseCheck order(String email, String isbn, boolean completed) {
        customers.add(email);
        orders.computeIfAbsent(email, e -> new HashMap<>()).put(isbn, completed);
        return this;
    }

    public synchronized InMemoryPurchaseCheck unavailable(boolean unavailable) {
        this.unavailable = unavailable;
        return this;
    }

    // run before each answer, e.g. to change the ratings table while a write waits on orders
    public synchronized InMemoryPurchaseCheck onCheck(Runnable hook) {
        this.hook = hook;
        return this;
    }

    public synchronized List<String> askedEmails() {
        return List.copyOf(askedEmails);
    }

    public synchronized void reset() {
        customers.clear();
        orders.clear();
        askedEmails.clear();
        unavailable = false;
        hook = null;
    }

    @Override
    public Eligibility check(String email, String isbn) {
        Runnable toRun;
        synchronized (this) {
            askedEmails.add(email);
            toRun = hook;
        }
        if (toRun != null) {
            toRun.run();
        }
        synchronized (this) {
            if (unavailable) {
                throw new OrdersUnavailableException(new ConnectException("orders unreachable"));
            }
            if (!customers.contains(email)) {
                return Eligibility.NOT_A_CUSTOMER;
            }
            Boolean completed = orders.getOrDefault(email, Map.of()).get(isbn);
            if (completed == null) {
                return Eligibility.NO_ORDER_FOR_BOOK;
            }
            return completed ? Eligibility.ELIGIBLE : Eligibility.ORDER_NOT_COMPLETED;
        }
    }
}
