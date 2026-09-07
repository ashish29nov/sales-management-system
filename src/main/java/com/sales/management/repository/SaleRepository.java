package com.sales.management.repository;

import com.sales.management.model.Sale;
import com.sales.management.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository
        extends JpaRepository<Sale, Long> {


    // =========================================================
    // SALESPERSON SALES
    // =========================================================

    List<Sale> findBySalespersonOrderBySaleDateDesc(
            User salesperson
    );


    // =========================================================
    // SALESPERSON SALES - DATE RANGE
    // =========================================================

    List<Sale>
    findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
            User salesperson,
            LocalDate startDate,
            LocalDate endDate
    );


    // =========================================================
    // ALL SALES - DATE RANGE
    // =========================================================

    List<Sale>
    findBySaleDateBetweenOrderBySaleDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );


    // =========================================================
    // ALL SALES OF SALESPERSON
    // =========================================================

    List<Sale> findBySalesperson(
            User salesperson
    );


    // =========================================================
    // CHECK DUPLICATE INVOICE NUMBER
    // =========================================================

    boolean existsByInvoiceNumber(
            String invoiceNumber
    );


    // =========================================================
    // TOTAL SALES FOR SALESPERSON + PERIOD
    // =========================================================
    //
    // IMPORTANT:
    // Old system:
    // Sale::getSaleAmount
    //
    // New system:
    // Sale::getTotalAmount
    //
    // Isliye target calculation ab invoice total
    // se automatically hogi.
    //

    @Query("""
            SELECT COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.salesperson = :salesperson
            AND s.saleDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal getTotalSalesForPeriod(
            @Param("salesperson") User salesperson,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

 // =========================================================
// TOTAL PRODUCT QUANTITY FOR SALESPERSON + PERIOD
// =========================================================

@Query("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM SaleItem i
        JOIN i.sale s
        WHERE s.salesperson = :salesperson
        AND s.saleDate BETWEEN :startDate AND :endDate
        """)
BigDecimal getTotalProductQuantityForPeriod(
        @Param("salesperson") User salesperson,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
);

@Query("""
        SELECT s
        FROM Sale s
        WHERE s.salesperson = :salesperson
        AND (:customer IS NULL OR :customer = ''
             OR LOWER(s.customerName) LIKE LOWER(CONCAT('%', :customer, '%')))
        AND (:startDate IS NULL OR s.saleDate >= :startDate)
        AND (:endDate IS NULL OR s.saleDate <= :endDate)
        ORDER BY s.saleDate DESC
        """)
List<Sale> findMySalesWithFilters(
        @Param("salesperson") User salesperson,
        @Param("customer") String customer,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
);


}