package com.dynatrace.ingest.model;

import com.dynatrace.ingest.model.Model;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import java.util.Random;

public class Storage implements Model {
    private long id;
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13. The book must exist in the books service")
    private String isbn;
    @Schema(name = "quantity", example = "20", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many pieces per ISBN to generate in a storage (0 stands for random)")
    private int quantity;
    private static final Random random = new Random();

    public Storage() {
    }

    public Storage(long id, String isbn, int quantity) {
        this.id = id;
        this.isbn = isbn;
        this.quantity = quantity;
    }

    public static Storage generate() {
        String isbn = Book.getRandomISBN();
        if (isbn == null) {
            return null;
        }
        return new Storage(0, isbn, random.nextInt(100) + 1);
    }

    public static void reset() {}

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

    @Override
    public String toString() {
        return this.isbn;
    }
}
