package com.archie.sentinel.controller;

import com.archie.sentinel.dto.AuditLogResponse;
import com.archie.sentinel.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{id}/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogResponse> history(@PathVariable Long id, Authentication auth) {
        return auditService.getIncidentHistory(id, auth.getName());
    }
}
