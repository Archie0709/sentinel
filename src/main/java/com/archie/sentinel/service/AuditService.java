package com.archie.sentinel.service;

import com.archie.sentinel.dto.AuditLogResponse;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.AuditLogRepository;
import com.archie.sentinel.repository.IncidentRepository;
import com.archie.sentinel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final String ADMIN = "ADMIN";

    private final AuditLogRepository auditLogRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        IncidentRepository incidentRepository,
                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getIncidentHistory(Long incidentId, String actorUsername) {
        requireAdmin(actorUsername);

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found: " + incidentId);
        }

        return auditLogRepository.findByIncidentIdOrderByIdDesc(incidentId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private void requireAdmin(String username) {
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!ADMIN.equals(actor.getRole().getName())) {
            throw new AccessDeniedException("Administrator access required");
        }
    }
}
