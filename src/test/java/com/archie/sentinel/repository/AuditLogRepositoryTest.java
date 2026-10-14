package com.archie.sentinel.repository;

import com.archie.sentinel.TestcontainersConfiguration;
import com.archie.sentinel.model.AuditLog;
import com.archie.sentinel.model.Incident;
import com.archie.sentinel.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuditLogRepositoryTest {

    @Autowired AuditLogRepository auditLogRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired SeverityRepository severityRepository;
    @Autowired IncidentStatusRepository statusRepository;

    private User analyst;
    private Incident incident;

    @BeforeEach
    void setUp() {
        var role = roleRepository.findByName("ANALYST").orElseThrow();
        analyst = userRepository.save(new User("analyst1", "hash", role));
        incident = incidentRepository.save(new Incident("title", "desc",
                severityRepository.findByName("LOW").orElseThrow(),
                statusRepository.findByName("PENDING").orElseThrow(),
                analyst));
    }

    @Test
    void findByIncidentId_returnsNewestFirst_andOnlyForThatIncident() {
        var other = incidentRepository.save(new Incident("other", "desc",
                severityRepository.findByName("LOW").orElseThrow(),
                statusRepository.findByName("PENDING").orElseThrow(),
                analyst));

        auditLogRepository.save(new AuditLog(incident, analyst, "severity", "LOW", "HIGH"));
        auditLogRepository.save(new AuditLog(incident, analyst, "status", "PENDING", "INVESTIGATING"));
        auditLogRepository.save(new AuditLog(other, analyst, "severity", "LOW", "MEDIUM"));

        var result = auditLogRepository.findByIncidentIdOrderByIdDesc(incident.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFieldName()).isEqualTo("status");   // written second, so first
        assertThat(result.get(1).getFieldName()).isEqualTo("severity");
    }

    @Test
    void auditLogRepository_exposesNoDeleteOrUpdateOperations() {
        var methodNames = Arrays.stream(AuditLogRepository.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.startsWith("delete"));
        assertThat(methodNames).containsExactlyInAnyOrder("save", "findByIncidentIdOrderByIdDesc");
    }
}
