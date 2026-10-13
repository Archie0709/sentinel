package com.archie.sentinel.repository;

import com.archie.sentinel.model.Incident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident,Long> {

    // Dashboard Counters
    long countByStatusNameNot(String statusName);   // Used to count all unresolved
    long countBySeverityRankOrderGreaterThanEqual(Short rankOrder); // Used to get count of high priority

    // Recently updated
    @EntityGraph(attributePaths = {"severity", "status", "assignedTo"})
    List<Incident> findTop5ByOrderByUpdatedAtDesc();    // 5 most recently updated
}
