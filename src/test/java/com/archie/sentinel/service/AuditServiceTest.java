package com.archie.sentinel.service;

import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.model.AuditLog;
import com.archie.sentinel.model.Incident;
import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.AuditLogRepository;
import com.archie.sentinel.repository.IncidentRepository;
import com.archie.sentinel.repository.UserRepository;
import com.archie.sentinel.security.CurrentUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static com.archie.sentinel.support.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock IncidentRepository incidentRepository;
    @Mock UserRepository userRepository;

    private AuditService service;

    private User admin;
    private User alice;

    @BeforeEach
    void setUp() {
        service = new AuditService(auditLogRepository, incidentRepository,
                new CurrentUserAccess(userRepository));

        admin = user(1, "admin", "ADMIN");
        alice = user(2, "alice", "ANALYST");
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void getIncidentHistory_byAdmin_returnsEntriesMappedToDtos() {
        Incident incident = incident(10, severity("MEDIUM"), status(1, "PENDING"), alice);
        var entry = new AuditLog(incident, alice, "severity", "MEDIUM", "HIGH");
        when(incidentRepository.existsById(10L)).thenReturn(true);
        when(auditLogRepository.findByIncidentIdOrderByIdDesc(10L)).thenReturn(List.of(entry));

        var result = service.getIncidentHistory(10L, "admin");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).changedBy()).isEqualTo("alice");
        assertThat(result.get(0).field()).isEqualTo("severity");
        assertThat(result.get(0).oldValue()).isEqualTo("MEDIUM");
        assertThat(result.get(0).newValue()).isEqualTo("HIGH");
    }

    @Test
    void getIncidentHistory_byAnalyst_isDenied_andReadsNothing() {
        assertThatThrownBy(() -> service.getIncidentHistory(10L, "alice"))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(auditLogRepository, incidentRepository);
    }

    @Test
    void getIncidentHistory_byUnknownActor_isDenied() {
        assertThatThrownBy(() -> service.getIncidentHistory(10L, "ghost"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getIncidentHistory_forMissingIncident_throwsNotFound() {
        when(incidentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getIncidentHistory(99L, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(auditLogRepository);
    }
}
