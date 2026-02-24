package com.dynatrace.books.model;

import com.dynatrace.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name="books", uniqueConstraints = @UniqueConstraint(columnNames = "isbn"))
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="isbn", nullable = false, unique = true, length = 13)
    @NotBlank
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13 code without dashes or spaces")
    private String isbn;

    @Column(name="title", nullable = false)
    @NotBlank
    @Schema(name = "title", example = "The Great Gatsby", requiredMode = Schema.RequiredMode.REQUIRED, description = "Book title")
    private String title;

    @Column(name="lang", nullable = false, length = 2)
    @NotBlank
    @Size(min = 2, max = 2)
    @Schema(name = "lang", example = "EN", requiredMode = Schema.RequiredMode.REQUIRED, description = "2-letters language code")
    private String language;

    @Column(name="published", nullable = false)
    @Schema(name = "published", example = "true", requiredMode = Schema.RequiredMode.AUTO, description = "Indicates whether the book is vendible (can be purchased)")
    private boolean published;

    @Column(name="author", nullable = false)
    @NotBlank
    @Schema(name = "author", example = "Frances Scott Fitzgerald", requiredMode = Schema.RequiredMode.REQUIRED, description = "Book author")
    private String author;

    @Column(name="price", nullable = false, precision = 10, scale = 2)
    @DecimalMin("0.01")
    @DecimalMax("999999.99")
    @Schema(name = "price", example = "32.12", requiredMode = Schema.RequiredMode.REQUIRED, description = "price (greater than 0; less than 1,000,000; 2 decimals)")
    private BigDecimal price;

//    @ManyToOne(fetch = FetchType.EAGER, optional = false)
//    @JoinColumn(name="author_id", nullable = false)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    @JsonIgnore
//    private Author author;
//
//    @ManyToOne(fetch = FetchType.EAGER, optional = false)
//    @JoinColumn(name="publisher_id", nullable = false)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    private Publisher publisher;

    public Book() {
    }

    public Book(long id, String isbn, String title, String author, String language, BigDecimal price, boolean published) {
        this.id = id;
        this.setIsbn(isbn);
        this.setLanguage(language);
        this.setPrice(price);
        this.title = title;
        this.author = author;
        this.published = published;
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
        if (isbn.length() != 13 || !isbn.matches("^\\d{13}$")) {
            throw new BadRequestException("ISBN must be a 13-digits value");
        }
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return this.price.setScale(2, RoundingMode.HALF_UP);
    }

    public void setPrice(BigDecimal price) {
        price = price.setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(new BigDecimal("1000000")) >= 0) {
            throw new BadRequestException("Invalid price. Must be between 0 and 1 million. Got: " + price);
        }
        this.price = price;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        if (language.length() != 2) {
            throw new BadRequestException("Country must be a 2-letter code");
        }
        this.language = language;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
