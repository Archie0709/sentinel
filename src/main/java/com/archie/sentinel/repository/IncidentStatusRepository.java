package com.archie.sentinel.repository;

import com.archie.sentinel.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentStatusRepository extends JpaRepository<IncidentStatus,Short> {
    Optional<IncidentStatus> findByName(String name);
}
