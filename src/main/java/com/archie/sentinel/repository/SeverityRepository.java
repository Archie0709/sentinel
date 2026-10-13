package com.archie.sentinel.repository;

import com.archie.sentinel.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeverityRepository extends JpaRepository<Severity,Short> {
    Optional<Severity> findByName(String name);
}
