package com.archie.sentinel.repository;

import com.archie.sentinel.TestcontainersConfiguration;
import com.archie.sentinel.model.Incident;
import com.archie.sentinel.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class IncidentRepositoryTest {

    @Autowired IncidentRepository incidentRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired SeverityRepository severityRepository;
    @Autowired IncidentStatusRepository incidentStatusRepository;

    private User analyst;

    @BeforeEach
    public void setup() {
        var role = roleRepository.findByName("ANALYST").orElseThrow();
        analyst = userRepository.save(new User("analyst1", "hash", role));
    }

    private Incident saveIncident(String title, String severity, String status) {
        return incidentRepository.save(new Incident(
            title, "desc",
            severityRepository.findByName(severity).orElseThrow(),
            incidentStatusRepository.findByName(status).orElseThrow(),
            analyst
        ));
    }

    @Test
    void countByStatusNameNot_excludesAccordingly() {
        saveIncident("a", "LOW", "PENDING");
        saveIncident("a", "LOW", "INVESTIGATING");
        saveIncident("a", "LOW", "RESOLVED");

        assertThat(incidentRepository.countByStatusNameNot("RESOLVED")).isEqualTo(2);
    }

    @Test
    void countBySeverityRankOrderGreaterEqualThan_countsIncludingAndAbove() {
        saveIncident("a", "LOW", "PENDING");
        saveIncident("a", "HIGH", "PENDING");
        saveIncident("a", "CRITICAL", "PENDING");

        assertThat(incidentRepository.countBySeverityRankOrderGreaterThanEqual((short) 3)).isEqualTo(2);
    }

    @Test
    void findTop5_returnsMax5() {
        for (int i = 0; i < 10; i++) {
            saveIncident("incident"+i, "LOW", "PENDING");
        }

        assertThat(incidentRepository.findTop5ByOrderByUpdatedAtDesc()).hasSize(5);
    }

    @Test
    void findTop5_returnsAllIfLessThan5() {
        saveIncident("incident1", "LOW", "PENDING");
        saveIncident("incident2", "LOW", "PENDING");

        assertThat(incidentRepository.findTop5ByOrderByUpdatedAtDesc()).hasSize(2);
    }

}
