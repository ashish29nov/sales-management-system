package com.sales.management.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesReport {

    private User salesperson;

    private BigDecimal target;

    private BigDecimal achieved;

    private BigDecimal remaining;

    private BigDecimal totalProductQuantity;

    private double achievementPercentage;

    // =========================================================
    // CLIENT PERFORMANCE
    // =========================================================

    private Integer clientTarget;

    private long actualClients;

    private long remainingClients;

    private double clientAchievementPercentage;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SalesReport(
            User salesperson,
            BigDecimal target,
            BigDecimal achieved,
            BigDecimal totalProductQuantity,
            Integer clientTarget,
            long actualClients) {

        this.salesperson = salesperson;


        // =====================================================
        // SALES TARGET
        // =====================================================

        this.target = target != null
                ? target
                : BigDecimal.ZERO;


        // =====================================================
        // SALES ACHIEVED
        // =====================================================

        this.achieved = achieved != null
                ? achieved
                : BigDecimal.ZERO;


        // =====================================================
        // TOTAL PRODUCT QUANTITY
        // =====================================================

        this.totalProductQuantity =
                totalProductQuantity != null
                        ? totalProductQuantity
                        : BigDecimal.ZERO;


        // =====================================================
        // SALES REMAINING
        // =====================================================

        this.remaining =
                this.target.subtract(this.achieved);

        // Remaining should never be negative
        if (this.remaining.compareTo(BigDecimal.ZERO) < 0) {

            this.remaining = BigDecimal.ZERO;

        }


        // =====================================================
        // SALES ACHIEVEMENT %
        // =====================================================

        if (this.target.compareTo(BigDecimal.ZERO) > 0) {

            this.achievementPercentage =
                    this.achieved
                            .divide(
                                    this.target,
                                    4,
                                    RoundingMode.HALF_UP
                            )
                            .doubleValue()
                            * 100;

        } else {

            this.achievementPercentage = 0;

        }


        // =====================================================
        // CLIENT TARGET
        // =====================================================

        this.clientTarget =
                clientTarget != null
                        ? clientTarget
                        : 0;


        // =====================================================
        // ACTUAL NEW CLIENTS
        // =====================================================

        this.actualClients =
                Math.max(actualClients, 0);


        // =====================================================
        // REMAINING CLIENTS
        // =====================================================

        this.remainingClients =
                this.clientTarget - this.actualClients;

        // Remaining clients should never be negative
        if (this.remainingClients < 0) {

            this.remainingClients = 0;

        }


        // =====================================================
        // CLIENT ACHIEVEMENT %
        // =====================================================

        if (this.clientTarget > 0) {

            this.clientAchievementPercentage =
                    ((double) this.actualClients
                            / this.clientTarget)
                            * 100;

        } else {

            this.clientAchievementPercentage = 0;

        }

    }


    // =========================================================
    // GETTERS
    // =========================================================

    public User getSalesperson() {

        return salesperson;

    }


    public BigDecimal getTarget() {

        return target;

    }


    public BigDecimal getAchieved() {

        return achieved;

    }


    public BigDecimal getRemaining() {

        return remaining;

    }


    public BigDecimal getTotalProductQuantity() {

        return totalProductQuantity;

    }


    public double getAchievementPercentage() {

        return achievementPercentage;

    }


    // =========================================================
    // CLIENT GETTERS
    // =========================================================

    public Integer getClientTarget() {

        return clientTarget;

    }


    public long getActualClients() {

        return actualClients;

    }


    public long getRemainingClients() {

        return remainingClients;

    }


    public double getClientAchievementPercentage() {

        return clientAchievementPercentage;

    }

}