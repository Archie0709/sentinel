package com.archie.sentinel.controller;

import com.archie.sentinel.dto.DashboardResponse;
import com.archie.sentinel.service.IncidentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final IncidentService incidentService;

    public DashboardController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public DashboardResponse dashboard(
            @RequestParam(defaultValue = "false") boolean includeResolved) {
        return incidentService.getDashboard(includeResolved);
    }
}
