package com.dynatrace.books.model;

import javax.persistence.*;

@Entity
@Table(name="publishers", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="name", nullable = false, unique = true)
    private String name;

    public Publisher() {
    }

    public Publisher(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
