package com.archie.sentinel.controller;

import com.archie.sentinel.dto.IncidentResponse;
import com.archie.sentinel.exception.InvalidStatusTransitionException;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
@WithMockUser(username = "alice")
class IncidentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IncidentService incidentService;

    private static IncidentResponse response() {
        return new IncidentResponse(10L, "Phishing", "desc", "HIGH", "PENDING",
                null, "alice", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_withValidBody_returns201AndUsesAuthenticatedUser() throws Exception {
        when(incidentService.createIncident("Phishing", "desc", "HIGH", "alice"))
                .thenReturn(response());

        mockMvc.perform(post("/api/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Phishing","description":"desc","severity":"HIGH"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/incidents/10"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_withBlankTitle_returns400_andNeverCallsService() throws Exception {
        mockMvc.perform(post("/api/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  ","description":"desc","severity":"HIGH"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verifyNoInteractions(incidentService);
    }

    @Test
    void create_ignoresClientSuppliedCreatedBy() throws Exception {
        when(incidentService.createIncident(any(), any(), any(), eq("alice")))
                .thenReturn(response());

        mockMvc.perform(post("/api/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","description":"d","severity":"LOW","createdBy":"admin"}"""))
                .andExpect(status().isCreated());

        verify(incidentService).createIncident("t", "d", "LOW", "alice");
    }

    @Test
    void changeStatus_invalidTransition_returns409() throws Exception {
        when(incidentService.changeStatus(10L, "RESOLVED", "alice"))
                .thenThrow(new InvalidStatusTransitionException("PENDING", "RESOLVED"));

        mockMvc.perform(patch("/api/incidents/10/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void changeSeverity_whenServiceDenies_returns403() throws Exception {
        when(incidentService.changeSeverity(anyLong(), any(), any()))
                .thenThrow(new AccessDeniedException("no"));

        mockMvc.perform(patch("/api/incidents/10/severity").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"severity":"HIGH"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_missingIncident_returns404() throws Exception {
        when(incidentService.getIncident(99L))
                .thenThrow(new ResourceNotFoundException("Incident not found: 99"));

        mockMvc.perform(get("/api/incidents/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/incidents/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void get_withoutLogin_isRejected() throws Exception {
        mockMvc.perform(get("/api/incidents/10"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void search_passesFiltersAndDefaults_andWrapsPageResponse() throws Exception {
        when(incidentService.searchIncidents("PENDING", "HIGH", null, false, 0, 20))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(response())));

        mockMvc.perform(get("/api/incidents")
                        .param("status", "PENDING").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

}
