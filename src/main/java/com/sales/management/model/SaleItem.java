package com.sales.management.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent Invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    // Product
    @Column(name = "product_name", nullable = false)
    private String productName;

    // Quantity
    @Column(
            name = "qty",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal quantity;

    // Unit
    @Column(name = "unit", nullable = false)
    private String unit;

    // Price
    @Column(
            name = "price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal price;

    // Amount
    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SaleItem() {
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    // =========================================================
    // CALCULATE ITEM AMOUNT
    // =========================================================

    public void calculateAmount() {

        if (quantity != null && price != null) {

            this.amount =
                    quantity.multiply(price);

        } else {

            this.amount =
                    BigDecimal.ZERO;
        }
    }
}