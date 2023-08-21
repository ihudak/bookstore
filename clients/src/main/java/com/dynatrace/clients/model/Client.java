package com.dynatrace.clients.model;

import com.dynatrace.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import javax.validation.constraints.Pattern;

@Entity
@Table(name="clients", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Client {
    private final String emailRegExp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="first_name", nullable = false)
    @Schema(name = "first_name", example = "Peter", requiredMode = Schema.RequiredMode.REQUIRED, description = "Client's first name")
    private String firstName;

    @Column(name="last_name", nullable = false)
    @Schema(name = "last_name", example = "Brown", requiredMode = Schema.RequiredMode.REQUIRED, description = "Client's last name")
    private String lastName;

    @Pattern(regexp = emailRegExp)
    @Column(name="email", nullable = false, unique = true)
    @Schema(name = "email", example = "pbrown@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, description = "Client's email address")
    private String email;

    public Client(long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.setEmail(email);
    }

    public Client() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!email.matches(this.emailRegExp)) {
            throw new BadRequestException("Invalid email address");
        }
        this.email = email;
    }
}
