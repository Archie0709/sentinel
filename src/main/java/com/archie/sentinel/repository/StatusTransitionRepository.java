package com.archie.sentinel.repository;

import com.archie.sentinel.model.StatusTransition;
import com.archie.sentinel.model.StatusTransitionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusTransitionRepository extends JpaRepository<StatusTransition, StatusTransitionId> {

}
