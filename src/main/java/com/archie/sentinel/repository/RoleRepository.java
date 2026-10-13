package com.archie.sentinel.repository;

import com.archie.sentinel.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Short> {
    Optional<Role> findByName(String name);
}
