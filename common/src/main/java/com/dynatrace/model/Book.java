package com.dynatrace.model;

public class Book implements Model {
    private long id;
    private String isbn;
    private String title;
    private String language;
    private boolean published;
    private String author;
    private double price;

    public Book() {
    }

    public Book(long id, String isbn, String title, String language, boolean published, String author, double price) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.language = language;
        this.published = published;
        this.author = author;
        this.price = price;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
