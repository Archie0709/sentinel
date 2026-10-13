package com.archie.sentinel.service;

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
import org.springframework.security.access.AccessDeniedException;

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
}