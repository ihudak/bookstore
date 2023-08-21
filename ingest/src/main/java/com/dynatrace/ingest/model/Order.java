package com.dynatrace.ingest.model;

import com.dynatrace.ingest.model.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Order implements Model {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private long id;
    @Pattern(regexp = emailRegExp)
    @Schema(name = "email", example = "pbrown@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, description = "email address. To process the order client must exist")
    private String email;
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13. To process the order the book must exist and be vendible")
    private String isbn;
    @Min(1)
    @Max(100)
    @Schema(name = "quantity", example = "14", requiredMode = Schema.RequiredMode.REQUIRED, description = "Number of the pieces to be purchased. To process the order storage must have enough pieces")
    private int quantity;
    @DecimalMin("0.01")
    @DecimalMax("999999.99")
    @Schema(name = "price", example = "32.12", requiredMode = Schema.RequiredMode.REQUIRED, description = "price. Order will fail if its price is less than in the books service")
    private double price;
    @Schema(name = "completed", example = "true", requiredMode = Schema.RequiredMode.AUTO, description = "Indicates whether the order is processed (purchase done)")
    private boolean completed;
    private static final Random random = new Random();
    private static final List<Order> orders = new ArrayList<>();
    private final static Logger logger = LoggerFactory.getLogger(Order.class);

    public Order() {
    }

    public Order(long id, String email, String isbn, int quantity, double price, boolean completed) {
        this.id = id;
        this.email = email;
        this.isbn = isbn;
        this.quantity = quantity;
        this.price = price;
        this.completed = completed;
    }

    public static Order generate() {
        String email = Client.getRandomEmail();
        String isbn = Book.getRandomISBN();
        logger.info("GENERATING ORDER");
        logger.info(isbn);
        logger.info(email);
        if (email == null || isbn == null) {
            logger.info("GENERATING ORDER FAILED");
            return null;
        }
        Order order = new Order(0, email, isbn, random.nextInt(3) + 1, 12.0, false);
        orders.add(order);
        logger.info(order.toString());
        return order;
    }

    public static Order getRandomOrder() {
        if (orders.isEmpty()) {
            return null;
        }
        int index = random.nextInt(orders.size());
        return orders.get(index);
    }

    public static void reset() {
        orders.clear();
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "ISBN: " + this.isbn + " Client: " + this.email;
    }
}
