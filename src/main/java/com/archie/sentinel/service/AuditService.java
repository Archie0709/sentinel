package com.archie.sentinel.service;

import com.archie.sentinel.dto.AuditLogResponse;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.repository.AuditLogRepository;
import com.archie.sentinel.repository.IncidentRepository;
import com.archie.sentinel.security.CurrentUserAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final IncidentRepository incidentRepository;
    private final CurrentUserAccess access;

    public AuditService(AuditLogRepository auditLogRepository,
                        IncidentRepository incidentRepository,
                        CurrentUserAccess access) {
        this.auditLogRepository = auditLogRepository;
        this.incidentRepository = incidentRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getIncidentHistory(Long incidentId, String actorUsername) {
        access.requireAdmin(actorUsername);

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found: " + incidentId);
        }

        return auditLogRepository.findByIncidentIdOrderByIdDesc(incidentId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
