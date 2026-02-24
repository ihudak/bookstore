package com.dynatrace.ratings.model;

import com.dynatrace.exception.BadRequestException;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name="ratings")
public class Rating {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="isbn", nullable = false, length = 13)
    private String isbn;

    @Column(name="rating", nullable = false)
    private int rating;

    @Column(name="comment", nullable = true)
    private String comment;

    @Column(name="updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name="created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Rating() {
        this.updatedAt = this.createdAt = new Date();
    }

    public Rating(long id, String email, String isbn, int rating, String comment) {
        this.id = id;
        this.setEmail(email);
        this.setIsbn(isbn);
        this.setRating(rating);
        this.comment = comment;
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating <= 0 || rating > 5) {
            throw new BadRequestException("Invalid rating. Must be between 0 and 5. Got: " + rating);
        }
        this.rating = rating;
        this.updatedAt = new Date();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
