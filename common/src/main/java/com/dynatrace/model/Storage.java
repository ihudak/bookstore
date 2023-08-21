package com.dynatrace.model;

public class Storage implements Model {
    private long id;
    private String isbn;
    private int quantity;

    public Storage() {
    }

    public Storage(long id, String isbn, int quantity) {
        this.id = id;
        this.isbn = isbn;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
