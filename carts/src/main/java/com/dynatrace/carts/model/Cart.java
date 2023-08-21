package com.dynatrace.carts.model;

import com.dynatrace.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.Date;

@Entity
@Table(name="cart", uniqueConstraints = @UniqueConstraint(columnNames = {"email","isbn"}))
public class Cart {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="email", nullable = false)
    @Pattern(regexp = emailRegExp)
    @Schema(name = "email", example = "pbrown@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, description = "email address. Writing to cart will fail if the client does not exit")
    private String email;

    @Column(name="isbn", nullable = false, length = 13)
    @Pattern(regexp = "^\\d{13}$")
    @Schema(name = "isbn", example = "9783161484100", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISBN13. writing to cart will fail if the book does not exit")
    private String isbn;

    @Min(1)
    @Max(100)
    @Column(name="quantity", nullable = false)
    @Schema(name = "quantity", example = "14", requiredMode = Schema.RequiredMode.REQUIRED, description = "Number of the pieces added to the cart")
    private int quantity;

    @Column(name="updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name="created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Cart() {
        this.updatedAt = this.createdAt = new Date();
    }

    public Cart(long id, String email, String isbn, int quantity) {
        this.id = id;
        this.setEmail(email);
        this.setIsbn(isbn);
        this.setQuantity(quantity);
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
