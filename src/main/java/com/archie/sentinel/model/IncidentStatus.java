package com.archie.sentinel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "incident_statuses")
public class IncidentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private short id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "sort_order", nullable = false, unique = true)
    private short sortOrder;

    protected IncidentStatus() {}

    public short getId() { return id; }
    public String getName() { return name; }
    public short getSortOrder() { return sortOrder; }

}
