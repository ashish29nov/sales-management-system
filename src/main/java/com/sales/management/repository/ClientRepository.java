package com.sales.management.repository;

import com.sales.management.model.Client;
import com.sales.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findBySalespersonOrderByCreatedDateDesc(User salesperson);

    List<Client> findBySalespersonAndCreatedDateBetweenOrderByCreatedDateDesc(
            User salesperson,
            LocalDate startDate,
            LocalDate endDate
    );

    long countBySalespersonAndCreatedDateBetween(
            User salesperson,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
        SELECT COUNT(c)
        FROM Client c
        WHERE c.salesperson = :salesperson
        AND c.createdDate BETWEEN :startDate AND :endDate
        """)
long countNewClientsForPeriod(
        @Param("salesperson") User salesperson,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
);


}