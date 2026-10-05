package com.archie.sentinel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "severities")
public class Severity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private short id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "rank_order", nullable = false, unique = true)
    private short rankOrder;

    protected Severity() {}

    public short getId() { return id; }
    public String getName() { return name; }
    public short getRankOrder() { return rankOrder; }

}
