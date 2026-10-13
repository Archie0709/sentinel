package com.archie.sentinel.service;

import com.archie.sentinel.dto.DashboardResponse;
import com.archie.sentinel.exception.InvalidStatusTransitionException;
import com.archie.sentinel.model.*;
import com.archie.sentinel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static com.archie.sentinel.support.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock IncidentRepository incidentRepository;
    @Mock UserRepository userRepository;
    @Mock SeverityRepository severityRepository;
    @Mock IncidentStatusRepository statusRepository;
    @Mock StatusTransitionRepository transitionRepository;
    @Mock AuditLogRepository auditLogRepository;

    @InjectMocks IncidentService service;

    private User admin;
    private User alice;   // analyst
    private User bob;     // analyst
    private Incident incident;

    @BeforeEach
    void setUp() {
        admin = user(1, "admin", "ADMIN");
        alice = user(2, "alice", "ANALYST");
        bob = user(3, "bob", "ANALYST");
        incident = incident(10, severity("MEDIUM"), status(1, "PENDING"), alice);
        incident.setAssignedTo(alice);

        lenient().when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        lenient().when(incidentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        lenient().when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @Test
    void changeStatus_rejectsTransitionNotInTable_andWritesNoAudit() {
        when(statusRepository.findByName("RESOLVED")).thenReturn(Optional.of(status(4, "RESOLVED")));
        when(transitionRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> service.changeStatus(10L, "RESOLVED", "alice"))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verifyNoInteractions(auditLogRepository);
        assertThat(incident.getStatus().getName()).isEqualTo("PENDING");
    }

    @Test
    void changeStatus_validTransition_updatesStatusAndWritesAudit() {
        when(statusRepository.findByName("INVESTIGATING")).thenReturn(Optional.of(status(2, "INVESTIGATING")));
        when(transitionRepository.existsById(any())).thenReturn(true);

        var result = service.changeStatus(10L, "INVESTIGATING", "alice");

        assertThat(result.status()).isEqualTo("INVESTIGATING");
        var captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getFieldName()).isEqualTo("status");
        assertThat(captor.getValue().getOldValue()).isEqualTo("PENDING");
        assertThat(captor.getValue().getNewValue()).isEqualTo("INVESTIGATING");
        assertThat(captor.getValue().getChangedBy()).isSameAs(alice);
    }

    @Test
    void analyst_cannotModifyIncidentAssignedToSomeoneElse() {
        assertThatThrownBy(() -> service.changeSeverity(10L, "HIGH", "bob"))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(auditLogRepository);
        assertThat(incident.getSeverity().getName()).isEqualTo("MEDIUM");
    }

    @Test
    void admin_canModifyAnyIncident() {
        when(severityRepository.findByName("HIGH")).thenReturn(Optional.of(severity("HIGH")));

        var result = service.changeSeverity(10L, "HIGH", "admin");

        assertThat(result.severity()).isEqualTo("HIGH");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void changeSeverity_toSameValue_writesNoAudit() {
        when(severityRepository.findByName("MEDIUM")).thenReturn(Optional.of(severity("MEDIUM")));

        service.changeSeverity(10L, "MEDIUM", "alice");

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void analyst_cannotAssignIncidentToAnotherUser() {
        assertThatThrownBy(() -> service.assign(10L, "bob", "alice"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDashboard_assemblesCountsAndRecentIncidents() {
        when(severityRepository.findByName("HIGH")).thenReturn(Optional.of(severityWithRank("HIGH", 3)));
        when(incidentRepository.count()).thenReturn(10L);
        when(incidentRepository.countByStatusNameNot("RESOLVED")).thenReturn(7L);
        when(incidentRepository.countBySeverityRankOrderGreaterThanEqual((short) 3)).thenReturn(4L);
        when(incidentRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of(incident));

        DashboardResponse result = service.getDashboard(true);

        assertThat(result.totalIncidents()).isEqualTo(10);
        assertThat(result.openIncidents()).isEqualTo(7);
        assertThat(result.highSeverityIncidents()).isEqualTo(4);
        assertThat(result.recentlyUpdated()).hasSize(1);
    }

    @Test
    void searchIncidents_blankFiltersBecomeNull_andPageSizeIsCapped() {
        when(incidentRepository.search(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(incident)));

        service.searchIncidents("  ", "", "alice", false, -5, 100_000);

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(incidentRepository).search(isNull(), isNull(), eq("alice"), eq(false), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void analyst_canModifyUnassignedIncident() {
        incident.setAssignedTo(null);
        when(severityRepository.findByName("HIGH")).thenReturn(Optional.of(severity("HIGH")));

        var result = service.changeSeverity(10L, "HIGH", "bob");

        assertThat(result.severity()).isEqualTo("HIGH");
    }

    @Test
    void analyst_canClaimUnassignedIncidentForThemselves() {
        incident.setAssignedTo(null);

        var result = service.assign(10L, "bob", "bob");

        assertThat(result.assignedTo()).isEqualTo("bob");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void searchIncidents_explicitResolvedStatusImpliesShowingResolved() {
        when(incidentRepository.search(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(incident)));

        service.searchIncidents("RESOLVED", null, null, false, 0, 10);

        verify(incidentRepository).search(eq("RESOLVED"), isNull(), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void getDashboard_excludingResolved_usesFilteredHighSeverityCount() {
        when(severityRepository.findByName("HIGH")).thenReturn(Optional.of(severityWithRank("HIGH", 3)));
        when(incidentRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of());
        when(incidentRepository.countBySeverityRankOrderGreaterThanEqualAndStatusNameNot((short) 3, "RESOLVED"))
                .thenReturn(2L);

        var result = service.getDashboard(false);

        assertThat(result.highSeverityIncidents()).isEqualTo(2);
    }

}