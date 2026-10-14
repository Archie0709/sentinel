package com.archie.sentinel.controller;

import com.archie.sentinel.dto.DashboardResponse;
import com.archie.sentinel.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@WithMockUser(username = "alice")
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IncidentService incidentService;

    @Test
    void dashboard_defaultsToHidingResolved() throws Exception {
        when(incidentService.getDashboard(false))
                .thenReturn(new DashboardResponse(10, 7, 2, List.of()));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").value(10))
                .andExpect(jsonPath("$.openIncidents").value(7))
                .andExpect(jsonPath("$.highSeverityIncidents").value(2));

        verify(incidentService).getDashboard(false);
    }

    @Test
    void dashboard_passesIncludeResolvedThrough() throws Exception {
        when(incidentService.getDashboard(true))
                .thenReturn(new DashboardResponse(10, 7, 4, List.of()));

        mockMvc.perform(get("/api/dashboard").param("includeResolved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highSeverityIncidents").value(4));

        verify(incidentService).getDashboard(true);
    }

    @Test
    void dashboard_withNonBooleanFlag_returns400() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("includeResolved", "maybe"))
                .andExpect(status().isBadRequest());
    }

}
