package com.archie.sentinel.repository;

import com.archie.sentinel.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Dashboard counters
    long countByStatusNameNot(String statusName);
    long countBySeverityRankOrderGreaterThanEqual(Short rankOrder);

    // Recently updated
    @EntityGraph(attributePaths = {"severity", "status", "assignedTo", "createdBy"})
    List<Incident> findTop5ByOrderByUpdatedAtDesc();

    // Filterable table: a null parameter means "don't filter on this"
    @EntityGraph(attributePaths = {"severity", "status", "assignedTo", "createdBy"})
    @Query(value = """
            SELECT i FROM Incident i
            LEFT JOIN i.assignedTo a
            WHERE (:status IS NULL OR i.status.name = :status)
              AND (:severity IS NULL OR i.severity.name = :severity)
              AND (:assignee IS NULL OR a.username = :assignee)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Incident i
            LEFT JOIN i.assignedTo a
            WHERE (:status IS NULL OR i.status.name = :status)
              AND (:severity IS NULL OR i.severity.name = :severity)
              AND (:assignee IS NULL OR a.username = :assignee)
            """)
    Page<Incident> search(@Param("status") String status,
                          @Param("severity") String severity,
                          @Param("assignee") String assignee,
                          Pageable pageable);
}