package com.dynatrace.storage.model;

import com.dynatrace.exception.BadRequestException;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="storage", uniqueConstraints = @UniqueConstraint(columnNames = "isbn"))
public class Storage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="isbn", nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(name="quantity", nullable = false)
    private int quantity;

    @Column(name="updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name="created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Storage() {
        this.updatedAt = this.createdAt = new Date();
    }

    public Storage(long id, String isbn, int quantity) {
        this.id = id;
        this.setIsbn(isbn);
        this.quantity = quantity;
        this.updatedAt = this.createdAt = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        this.updatedAt = new Date();
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) throws BadRequestException {
        if (isbn.length() != 13 || !isbn.matches("^\\d{13}$")) {
            throw new BadRequestException("ISBN must be a 13-digits value");
        }
        this.isbn = isbn;
        this.updatedAt = new Date();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) throws BadRequestException {
        if (quantity < 0) {
            throw new BadRequestException("Quantity must be greater than zero. Got: " + quantity);
        }
        this.quantity = quantity;
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
