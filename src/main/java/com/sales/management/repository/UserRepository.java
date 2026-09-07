package com.sales.management.repository;

import com.sales.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Login / user lookup
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Existing AdminController methods
    long countByRole(String role);

    long countByRoleAndActive(String role, boolean active);

    List<User> findByRole(String role);

    // Useful existing methods
    List<User> findByRoleAndActive(String role, boolean active);

    Optional<User> findByEmployeeId(String employeeId);
}