package com.archie.sentinel.dto;

import com.archie.sentinel.model.Incident;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id, String title, String description,
        String severity, String status,
        String assignedTo, String createdBy,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {

    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(
                i.getId(), i.getTitle(), i.getDescription(),
                i.getSeverity().getName(), i.getStatus().getName(),
                i.getAssignedTo() == null ? null : i.getAssignedTo().getUsername(),
                i.getCreatedBy().getUsername(),
                i.getCreatedAt(), i.getUpdatedAt());
    }

}
