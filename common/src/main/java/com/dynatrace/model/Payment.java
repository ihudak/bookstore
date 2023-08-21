package com.dynatrace.model;

public class Payment implements Model {
    public Payment() {
    }

    public Payment(long orderId, double amount, String email) {
        this.orderId = orderId;
        this.amount = amount;
        this.email = email;
    }

    public Payment(long orderId, double amount, String email, boolean succeeded, String message) {
        this.orderId = orderId;
        this.amount = amount;
        this.email = email;
        this.succeeded = succeeded;
        this.message = message;
    }

    @Override
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

    private long orderId;
    private double amount;
    private String email;
    private boolean succeeded;
    private String message;
}
