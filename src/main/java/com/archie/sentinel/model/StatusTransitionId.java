package com.archie.sentinel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StatusTransitionId implements Serializable {

    @Column(name = "from_status_id")
    private Short fromStatusId;

    @Column(name = "to_status_id")
    private Short toStatusId;

    protected StatusTransitionId() {}

    public StatusTransitionId(Short fromStatusId, Short toStatusId) {
        this.fromStatusId = fromStatusId;
        this.toStatusId = toStatusId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StatusTransitionId other
                && Objects.equals(fromStatusId, other.fromStatusId)
                && Objects.equals(toStatusId, other.toStatusId);
    }

    @Override
    public int hashCode() { return Objects.hash(fromStatusId, toStatusId); }

}
