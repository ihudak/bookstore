package com.dynatrace.model;

public class DynaPay implements Model {
    private long orderId;
    private double amount;
    private String email;
    private boolean succeeded;
    private String message;

    public DynaPay() {
    }

    public DynaPay(long orderId, double amount, String email, boolean succeeded, String message) {
        this.orderId = orderId;
        this.amount = amount;
        this.email = email;
        this.succeeded = succeeded;
        this.message = message;
    }

    public DynaPay(long orderId, double amount, String email) {
        this.orderId = orderId;
        this.amount = amount;
        this.email = email;
        this.succeeded = true;
        this.message = "";
    }

    public long getId() {
        return this.getOrderId();
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
