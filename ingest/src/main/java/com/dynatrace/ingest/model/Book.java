package com.dynatrace.ingest.model;
import com.github.javafaker.Faker;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Random;

public class Book implements Model {
    private long id;
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13 code without dashes or spaces")
    private String isbn;
    @Schema(name = "title", example = "The Great Gatsby", requiredMode = Schema.RequiredMode.REQUIRED, description = "Book title")
    private String title;
    @Size(min = 2, max = 2)
    @Schema(name = "lang", example = "EN", requiredMode = Schema.RequiredMode.REQUIRED, description = "2-letters language code")
    private String language;
    @Schema(name = "published", example = "true", requiredMode = Schema.RequiredMode.AUTO, description = "Indicates whether the book is vendible (can be purchased)")
    private boolean published;
    @Schema(name = "author", example = "Frances Scott Fitzgerald", requiredMode = Schema.RequiredMode.REQUIRED, description = "Book author")
    private String author;
    @DecimalMin("0.01")
    @DecimalMax("999999.99")
    @Schema(name = "price", example = "32.12", requiredMode = Schema.RequiredMode.REQUIRED, description = "price (greater than 0; less than 1,000,000; 2 decimals)")
    private double price;

    private static Long currentISBN = 10000000000000L;
    private static final Long startISBN = 10000000000000L;
    private static final double maxRandPrice = 33.0;

    public static Long getCurrentISBN() {
        return currentISBN;
    }

    private static final Faker faker = new Faker();
    private static final Random random = new Random();

    static public Book generate() {
        return generate(faker.bool().bool());
    }
    static public Book generate(boolean vend) {
        return generate(vend, random.nextDouble(maxRandPrice));
    }

    static public Book generate(double price) {
        return generate(faker.bool().bool(), price);
    }

    static public Book generate(boolean vend, double price) {
        return new Book(0, (--currentISBN).toString(), faker.book().title(), "EN", vend, faker.book().author(), price);
    }

    static public String getRandomISBN() {
        if (Objects.equals(currentISBN, startISBN)) {
            return null;
        }
        long isbn = random.nextLong(startISBN - currentISBN) + currentISBN;
        return Long.toString(isbn);
    }

    static public long getNumOfISBNs() {
        return startISBN - currentISBN;
    }

    static public void reset() {
        currentISBN = startISBN;
    }

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

    @Override
    public String toString() {
        return this.isbn;
    }
}
