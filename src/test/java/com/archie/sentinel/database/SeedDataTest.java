package com.archie.sentinel.database;

import com.archie.sentinel.TestcontainersConfiguration;
import com.archie.sentinel.model.IncidentStatus;
import com.archie.sentinel.model.Role;
import com.archie.sentinel.model.Severity;
import com.archie.sentinel.repository.IncidentStatusRepository;
import com.archie.sentinel.repository.RoleRepository;
import com.archie.sentinel.repository.SeverityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class SeedDataTest {

    @Autowired RoleRepository roleRepository;
    @Autowired SeverityRepository severityRepository;
    @Autowired IncidentStatusRepository incidentStatusRepository;

    @Test
    void rolesAreSeeded() {
        assertThat(roleRepository.findAll())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ANALYST", "ADMIN");
    }

    @Test
    void severitiesAreSeededInRankOrder() {
        assertThat(severityRepository.findAll())
                .extracting(Severity::getName, Severity::getRankOrder)
                .containsExactlyInAnyOrder(
                        tuple("LOW", (short) 1), tuple("MEDIUM", (short) 2),
                        tuple("HIGH", (short) 3), tuple("SEVERE", (short) 4),
                        tuple("CRITICAL", (short) 5));
    }

    @Test
    void statusesAreSeeded() {
        assertThat(incidentStatusRepository.findAll())
                .extracting(IncidentStatus::getName)
                .containsExactlyInAnyOrder("PENDING", "INVESTIGATING", "CONTAINED", "RESOLVED");
    }

}
