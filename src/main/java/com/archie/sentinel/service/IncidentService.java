package com.archie.sentinel.service;

import com.archie.sentinel.dto.IncidentResponse;
import com.archie.sentinel.exception.InvalidStatusTransitionException;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.model.*;
import com.archie.sentinel.repository.*;
import com.archie.sentinel.security.CurrentUserAccess;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.archie.sentinel.dto.DashboardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

import java.util.Objects;

@Service
public class IncidentService {

    private static final String INITIAL_STATUS = "PENDING";
    private static final String RESOLVED_STATUS = "RESOLVED";
    private static final String HIGH_SEVERITY = "HIGH";
    private static final int MAX_PAGE_SIZE = 100;

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final SeverityRepository severityRepository;
    private final IncidentStatusRepository statusRepository;
    private final StatusTransitionRepository transitionRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserAccess access;

    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           SeverityRepository severityRepository,
                           IncidentStatusRepository statusRepository,
                           StatusTransitionRepository transitionRepository,
                           AuditLogRepository auditLogRepository,
                           CurrentUserAccess access) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.severityRepository = severityRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.auditLogRepository = auditLogRepository;
        this.access = access;
    }

    @Transactional
    public IncidentResponse createIncident(String title, String description, String severityName, String actorUsername) {
        User actor = access.requireUser(actorUsername);
        Severity severity = loadSeverity(severityName);
        IncidentStatus status = statusRepository.findByName(INITIAL_STATUS)
                .orElseThrow(() -> new ResourceNotFoundException("Missing status " + INITIAL_STATUS));

        Incident saved = incidentRepository.saveAndFlush(
                new Incident(title, description, severity, status, actor));
        audit(saved, actor, "created", null, saved.getTitle());
        return IncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long id) {
        return IncidentResponse.from(loadIncident(id));
    }

    @Transactional
    public IncidentResponse changeSeverity(Long incidentId, String severityName, String actorUsername) {
        User actor = access.requireUser(actorUsername);
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
    public IncidentResponse changeStatus(Long incidentId, String statusName, String actorUsername) {
        User actor = access.requireUser(actorUsername);
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
    public IncidentResponse assign(Long incidentId, String assigneeUsername, String actorUsername) {
        User actor = access.requireUser(actorUsername);
        Incident incident = loadIncident(incidentId);
        assertCanModify(incident, actor);

        User assignee = loadAssignee(assigneeUsername);
        if (!access.isAdmin(actor) && !Objects.equals(assignee.getId(), actor.getId())) {
            throw new AccessDeniedException("Analysts can only assign incidents to themselves");
        }

        String oldName = incident.getAssignedTo() == null
                ? null : incident.getAssignedTo().getUsername();
        incident.setAssignedTo(assignee);
        audit(incident, actor, "assigned_to", oldName, assignee.getUsername());
        return IncidentResponse.from(incidentRepository.saveAndFlush(incident));
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(boolean includeResolved) {
        Severity high = loadSeverity(HIGH_SEVERITY);

        long total = incidentRepository.count();
        long open = incidentRepository.countByStatusNameNot(RESOLVED_STATUS);
        long highAndAbove = includeResolved
                ? incidentRepository.countBySeverityRankOrderGreaterThanEqual(high.getRankOrder())
                : incidentRepository.countBySeverityRankOrderGreaterThanEqualAndStatusNameNot(
                        high.getRankOrder(), RESOLVED_STATUS);

        List<IncidentResponse> recent = incidentRepository.findTop5ByOrderByUpdatedAtDesc()
                .stream()
                .map(IncidentResponse::from)
                .toList();

        return new DashboardResponse(total, open, highAndAbove, recent);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> searchIncidents(String status, String severity, String assignee, boolean includeResolved, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String statusFilter = blankToNull(status);
        // Explicitly asking for RESOLVED incidents implies showing them
        boolean showResolved = includeResolved || RESOLVED_STATUS.equals(statusFilter);

        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));

        return incidentRepository
                .search(statusFilter, blankToNull(severity), blankToNull(assignee),
                        showResolved, pageable)
                .map(IncidentResponse::from);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Helper methods

    private void assertCanModify(Incident incident, User actor) {
        if (access.isAdmin(actor)) return;
        User assignee = incident.getAssignedTo();
        boolean unassigned = assignee == null;
        boolean assignedToActor = assignee != null && Objects.equals(assignee.getId(), actor.getId());
        if (!unassigned && !assignedToActor) {
            throw new AccessDeniedException("You do not have permission to modify this incident");
        }
    }

    private User loadAssignee(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void audit(Incident incident, User actor, String field, String oldValue, String newValue) {
        auditLogRepository.save(new AuditLog(incident, actor, field, oldValue, newValue));
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