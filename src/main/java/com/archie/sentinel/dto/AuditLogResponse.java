package com.archie.sentinel.dto;

import com.archie.sentinel.model.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id, String changedBy, String field,
        String oldValue, String newValue, LocalDateTime changedAt) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(), a.getChangedBy().getUsername(), a.getFieldName(),
                a.getOldValue(), a.getNewValue(), a.getChangedAt());
    }
}
