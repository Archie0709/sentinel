package com.archie.sentinel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private short id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    protected Role() {}

    public short getId() { return id; }
    public String getName() { return name; }

}
