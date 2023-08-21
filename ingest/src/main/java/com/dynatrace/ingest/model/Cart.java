package com.dynatrace.ingest.model;


import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.Random;

public class Cart implements Model {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private long id;
    @Pattern(regexp = emailRegExp)
    @Schema(name = "email", example = "pbrown@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, description = "email address. writing to cart will fail for a non-existing client")
    private String email;
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13 code without dashes or spaces. writing to cart will fail for a non-existing book")
    private String isbn;
    @Min(1)
    @Max(100)
    @Schema(name = "quantity", example = "14", requiredMode = Schema.RequiredMode.REQUIRED, description = "Number of the pieces added to the cart")
    private int quantity;
    private static final Random random = new Random();

    public Cart() {
    }

    public Cart(long id, String email, String isbn, int quantity) {
        this.id = id;
        this.email = email;
        this.isbn = isbn;
        this.quantity = quantity;
    }

    public static Cart generate() {
        String email = Client.getRandomEmail();
        String isbn = Book.getRandomISBN();
        if (email == null || isbn == null) {
            return null;
        }
        return new Cart(0, email, isbn, random.nextInt(3) + 1);
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Book " + this.isbn + " client " + this.email;
    }
}
