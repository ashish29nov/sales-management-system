package com.sales.management.repository;

import com.sales.management.model.Target;
import com.sales.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TargetRepository extends JpaRepository<Target, Long> {

    // Existing target-month based functionality
    Optional<Target> findBySalespersonAndTargetMonth(
            User salesperson,
            String targetMonth
    );

    List<Target> findBySalespersonOrderByTargetMonthDesc(
            User salesperson
    );

    List<Target> findByTargetMonthOrderByTargetAmountDesc(
            String targetMonth
    );

    List<Target> findByTargetMonth(
            String targetMonth
    );

    // Admin / reporting support
    List<Target> findBySalesperson(
            User salesperson
    );
}