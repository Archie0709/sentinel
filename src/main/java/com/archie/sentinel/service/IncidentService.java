package com.archie.sentinel.service;

import com.archie.sentinel.dto.IncidentResponse;
import com.archie.sentinel.exception.InvalidStatusTransitionException;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.model.*;
import com.archie.sentinel.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class IncidentService {

    private static final String ADMIN = "ADMIN";
    private static final String INITIAL_STATUS = "PENDING";

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final SeverityRepository severityRepository;
    private final IncidentStatusRepository statusRepository;
    private final StatusTransitionRepository transitionRepository;
    private final AuditLogRepository auditLogRepository;

    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           SeverityRepository severityRepository,
                           IncidentStatusRepository statusRepository,
                           StatusTransitionRepository transitionRepository,
                           AuditLogRepository auditLogRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.severityRepository = severityRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public IncidentResponse createIncident(String title, String description,
                                           String severityName, String actorUsername) {
        User actor = loadUser(actorUsername);
        Severity severity = loadSeverity(severityName);
        IncidentStatus status = statusRepository.findByName(INITIAL_STATUS)
                .orElseThrow(() -> new ResourceNotFoundException("Missing status " + INITIAL_STATUS));

        Incident saved = incidentRepository.saveAndFlush(
                new Incident(title, description, severity, status, actor));
        return IncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long id) {
        return IncidentResponse.from(loadIncident(id));
    }

    @Transactional
    public IncidentResponse changeSeverity(Long incidentId, String severityName,
                                           String actorUsername) {
        User actor = loadUser(actorUsername);
        Incident incident = loadIncident(incidentId);
        assertCanModify(incident, actor);

        Severity newSeverity = loadSeverity(severityName);
        String oldName = incident.getSeverity().getName();
        if (!oldName.equals(newSeverity.getName())) {
            incident.setSeverity(newSeverity);
            audit(incident, actor, "severity", oldName, newSeverity.getName());
        }
        return IncidentResponse.from(incidentRepository.saveAndFlush(incident));
    }

    @Transactional
    public IncidentResponse changeStatus(Long incidentId, String statusName,
                                         String actorUsername) {
        User actor = loadUser(actorUsername);
        Incident incident = loadIncident(incidentId);
        assertCanModify(incident, actor);

        IncidentStatus target = statusRepository.findByName(statusName)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown status " + statusName));
        IncidentStatus current = incident.getStatus();

        var transition = new StatusTransitionId(current.getId(), target.getId());
        if (!transitionRepository.existsById(transition)) {
            throw new InvalidStatusTransitionException(current.getName(), target.getName());
        }

        incident.setStatus(target);
        audit(incident, actor, "status", current.getName(), target.getName());
        return IncidentResponse.from(incidentRepository.saveAndFlush(incident));
    }

    @Transactional
    public IncidentResponse assign(Long incidentId, String assigneeUsername,
                                   String actorUsername) {
        User actor = loadUser(actorUsername);
        Incident incident = loadIncident(incidentId);
        assertCanModify(incident, actor);

        User assignee = loadUser(assigneeUsername);
        if (!isAdmin(actor) && !Objects.equals(assignee.getId(), actor.getId())) {
            throw new AccessDeniedException("Analysts can only assign incidents to themselves");
        }

        String oldName = incident.getAssignedTo() == null
                ? null : incident.getAssignedTo().getUsername();
        incident.setAssignedTo(assignee);
        audit(incident, actor, "assigned_to", oldName, assignee.getUsername());
        return IncidentResponse.from(incidentRepository.saveAndFlush(incident));
    }

    // Helper methods

    private void assertCanModify(Incident incident, User actor) {
        if (isAdmin(actor)) return;
        User assignee = incident.getAssignedTo();
        boolean unassigned = assignee == null;
        boolean assignedToActor = assignee != null && Objects.equals(assignee.getId(), actor.getId());
        if (!unassigned && !assignedToActor) {
            throw new AccessDeniedException("You do not have permission to modify this incident");
        }
    }

    private boolean isAdmin(User user) {
        return ADMIN.equals(user.getRole().getName());
    }

    private void audit(Incident incident, User actor, String field, String oldValue, String newValue) {
        auditLogRepository.save(new AuditLog(incident, actor, field, oldValue, newValue));
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Incident loadIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private Severity loadSeverity(String name) {
        return severityRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown severity " + name));
    }
}