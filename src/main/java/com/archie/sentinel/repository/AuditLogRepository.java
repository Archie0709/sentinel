package com.archie.sentinel.repository;

import com.archie.sentinel.model.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findByIncidentIdOrderByIdDesc(Long incidentId);
}
