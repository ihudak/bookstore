package com.dynatrace.orders.model;

import com.dynatrace.exception.BadRequestException;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="orders", uniqueConstraints = @UniqueConstraint(columnNames = {"email","isbn"}))
public class Order {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="isbn", nullable = false, length = 13)
    private String isbn;

    @Column(name="quantity", nullable = false)
    private int quantity;

    @Column(name="price", nullable = false, precision = 10, scale = 2)
    private double price;

    @Column(name="completed", nullable = false)
    private boolean completed;

    @Column(name="updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name="created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Order() {
        this.updatedAt = this.createdAt = new Date();
    }

    public Order(long id, String email, String isbn, int quantity, double price, boolean completed) {
        this.id = id;
        this.setEmail(email);
        this.setIsbn(isbn);
        this.setQuantity(quantity);
        this.setPrice(price);
        this.completed = completed;
        this.updatedAt = this.createdAt = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        this.updatedAt = new Date();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!email.matches(this.emailRegExp)) {
            throw new BadRequestException("Invalid email address");
        }
        this.email = email;
        this.updatedAt = new Date();
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        if (isbn.length() != 13 || !isbn.matches("^\\d{13}$")) {
            throw new BadRequestException("ISBN must be a 13-digits value");
        }
        this.isbn = isbn;
        this.updatedAt = new Date();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero. Got: " + quantity);
        }
        this.quantity = quantity;
        this.updatedAt = new Date();
    }

    public double getPrice() {
        return (double) Math.round(this.price * 100.0) / 100.0;
    }

    public void setPrice(double price) {
        price = (double) Math.round(price * 100.0) / 100.0;
        if (price < 0 || price >= 1000000 || this.completed && price == 0) {
            throw new BadRequestException("Invalid price. Must be between 0 and 1 million. Got: " + price);
        }
        this.price = price;
        this.updatedAt = new Date();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        if (this.price <= 0 && completed) {
            throw new BadRequestException("Cannot complete the order while price is 0");
        }
        this.completed = completed;
        this.updatedAt = new Date();
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
    }
}
