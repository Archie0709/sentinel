package com.archie.sentinel.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "status_transitions")
public class StatusTransition {

    @EmbeddedId
    private StatusTransitionId id;

    protected StatusTransition() {}

    public StatusTransitionId getId() { return id; }

}
