package com.sales.management.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "targets")
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesperson_id", nullable = false)
    private User salesperson;

    @Column(name = "target_month", nullable = false)
    private String targetMonth;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount = BigDecimal.ZERO;

    // =========================================================
    // NEW CLIENT TARGET
    // =========================================================

    @Column(name = "client_target", nullable = false)
    private Integer clientTarget = 0;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Target() {
    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSalesperson() {
        return salesperson;
    }

    public void setSalesperson(User salesperson) {
        this.salesperson = salesperson;
    }

    public String getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(String targetMonth) {
        this.targetMonth = targetMonth;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Integer getClientTarget() {
        return clientTarget;
    }

    public void setClientTarget(Integer clientTarget) {
        this.clientTarget = clientTarget;
    }
}