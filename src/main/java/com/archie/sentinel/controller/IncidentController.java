package com.archie.sentinel.controller;

import com.archie.sentinel.dto.*;
import com.archie.sentinel.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request,
                                                   Authentication auth) {
        IncidentResponse created = incidentService.createIncident(
                request.title(), request.description(), request.severity(), auth.getName());
        return ResponseEntity.created(URI.create("/api/incidents/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<IncidentResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "false") boolean includeResolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(incidentService.searchIncidents(
                status, severity, assignee, includeResolved, page, size));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id) {
        return incidentService.getIncident(id);
    }

    @PatchMapping("/{id}/severity")
    public IncidentResponse changeSeverity(@PathVariable Long id,
                                           @Valid @RequestBody ChangeSeverityRequest request,
                                           Authentication auth) {
        return incidentService.changeSeverity(id, request.severity(), auth.getName());
    }

    @PatchMapping("/{id}/status")
    public IncidentResponse changeStatus(@PathVariable Long id,
                                         @Valid @RequestBody ChangeStatusRequest request,
                                         Authentication auth) {
        return incidentService.changeStatus(id, request.status(), auth.getName());
    }

    @PatchMapping("/{id}/assignee")
    public IncidentResponse assign(@PathVariable Long id,
                                   @Valid @RequestBody AssignRequest request,
                                   Authentication auth) {
        return incidentService.assign(id, request.assignee(), auth.getName());
    }
}
