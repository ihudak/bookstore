package com.dynatrace.ratings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_ratings_email_isbn", columnNames = {"email", "isbn"}),
        indexes = @Index(name = "idx_ratings_isbn", columnList = "isbn"))
public class Rating {
    public static final String EMAIL_PATTERN = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private final String emailRegExp = EMAIL_PATTERN;
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

    // a new rating: Created At and Updated At are both the time it is given
    public Rating(String email, String isbn, int stars, String comment, Date at) {
        this.email = email;
        this.isbn = isbn;
        this.rating = stars;
        this.comment = commentOrNull(comment);
        this.createdAt = at;
        this.updatedAt = at;
    }

    // a change alters only the stars, the comment and Updated At
    public void applyChange(int stars, String comment, Date at) {
        this.rating = stars;
        this.comment = commentOrNull(comment);
        this.updatedAt = at;
    }

    private static String commentOrNull(String comment) {
        return (comment == null || comment.isBlank()) ? null : comment;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    // comments are stored, and returned by no answer of the ratings service
    @JsonIgnore
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
