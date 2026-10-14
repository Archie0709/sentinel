package com.archie.sentinel.controller;

import com.archie.sentinel.dto.AuditLogResponse;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@WithMockUser(username = "admin")
class AuditControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditService auditService;

    @Test
    void history_returnsEntries() throws Exception {
        when(auditService.getIncidentHistory(10L, "admin")).thenReturn(List.of(
                new AuditLogResponse(1L, "alice", "severity", "MEDIUM", "HIGH", LocalDateTime.now())));

        mockMvc.perform(get("/api/incidents/10/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].changedBy").value("alice"))
                .andExpect(jsonPath("$[0].field").value("severity"))
                .andExpect(jsonPath("$[0].oldValue").value("MEDIUM"))
                .andExpect(jsonPath("$[0].newValue").value("HIGH"));
    }

    @Test
    void history_whenServiceDenies_returns403() throws Exception {
        when(auditService.getIncidentHistory(10L, "admin"))
                .thenThrow(new AccessDeniedException("Administrator access required"));

        mockMvc.perform(get("/api/incidents/10/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    void history_forMissingIncident_returns404() throws Exception {
        when(auditService.getIncidentHistory(99L, "admin"))
                .thenThrow(new ResourceNotFoundException("Incident not found: 99"));

        mockMvc.perform(get("/api/incidents/99/audit"))
                .andExpect(status().isNotFound());
    }

    @Test
    void auditTrail_cannotBeWrittenToOverHttp() throws Exception {
        mockMvc.perform(post("/api/incidents/10/audit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/incidents/10/audit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/incidents/10/audit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/incidents/10/audit").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

}
