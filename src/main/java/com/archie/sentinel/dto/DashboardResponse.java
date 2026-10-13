package com.archie.sentinel.dto;

import java.util.List;

public record DashboardResponse(
        long totalIncidents,
        long openIncidents,
        long highSeverityIncidents,
        List<IncidentResponse> recentlyUpdated) {}