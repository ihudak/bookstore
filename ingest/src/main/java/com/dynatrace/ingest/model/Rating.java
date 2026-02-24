package com.dynatrace.ingest.model;

import com.dynatrace.ingest.model.Model;
import com.github.javafaker.Faker;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import java.util.Random;

public class Rating implements Model {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private long id;
    @Pattern(regexp = emailRegExp)
    @Schema(name = "email", example = "pbrown@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, description = "email address. ratings will succeed even for non-existing clients")
    private String email;
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13 code without dashes or spaces. rating will succeed even for non-existing books")
    private String isbn;
    private int rating;
    @Schema(name = "comment", example = "I really enjoyed reading it!", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Free text comment")
    private String comment;
    private static final Random random = new Random();
    private static final Faker faker = new Faker();

    public Rating() {
    }

    public Rating(long id, String email, String isbn, int rating, String comment) {
        this.id = id;
        this.email = email;
        this.isbn = isbn;
        this.rating = rating;
        this.comment = comment;
    }

    public static Rating generate() {
        String email = Client.getRandomEmail();
        String isbn = Book.getRandomISBN();
        if (email == null || isbn == null) {
            return null;
        }
        return new Rating(0, email, isbn, random.nextInt(4) + 1, faker.lorem().characters(true));
    }

    public static void reset() {}

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "ISBN: " + this.isbn + " Client: " + this.email;
    }
}
