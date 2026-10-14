package com.archie.sentinel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, updatable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false, updatable = false)
    private User changedBy;

    @Column(name = "field_name", nullable = false, updatable = false, length = 50)
    private String fieldName;

    @Column(name = "old_value", updatable = false, length = 500)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 500)
    private String newValue;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected AuditLog() {}

    public AuditLog(Incident incident, User user, String fieldName, String oldValue, String newValue) {
        this.incident = incident;
        this.changedBy = user;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getChangedBy() { return changedBy; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public LocalDateTime getChangedAt() { return changedAt; }

}
