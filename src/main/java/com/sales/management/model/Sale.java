package com.sales.management.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // SALESPERSON
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesperson_id", nullable = false)
    private User salesperson;

    // =========================================================
    // INVOICE / BILL
    // =========================================================

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    // =========================================================
    // SALE DATE
    // =========================================================

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    // =========================================================
    // CUSTOMER
    // =========================================================

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    // =========================================================
    // OLD FIELDS - KEPT FOR EXISTING MODULE COMPATIBILITY
    // =========================================================

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(
            name = "sale_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saleAmount = BigDecimal.ZERO;

    // =========================================================
    // NEW INVOICE TOTAL
    // =========================================================

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // =========================================================
    // MULTIPLE PRODUCTS
    // =========================================================

    @OneToMany(
            mappedBy = "sale",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SaleItem> items = new ArrayList<>();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Sale() {
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // OLD PRODUCT NAME

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    // OLD SALE AMOUNT

    public BigDecimal getSaleAmount() {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount) {
        this.saleAmount = saleAmount;
    }

    // NEW TOTAL AMOUNT

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // ITEMS

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    // =========================================================
    // ADD ITEM
    // =========================================================

    public void addItem(SaleItem item) {

        if (item == null) {
            return;
        }

        items.add(item);
        item.setSale(this);

        calculateTotal();
    }

    // =========================================================
    // REMOVE ITEM
    // =========================================================

    public void removeItem(SaleItem item) {

        if (item == null) {
            return;
        }

        items.remove(item);
        item.setSale(null);

        calculateTotal();
    }

    // =========================================================
    // CALCULATE TOTAL
    // =========================================================

    public void calculateTotal() {

        this.totalAmount =
                items.stream()
                        .map(SaleItem::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        // Keep old field synchronized
        this.saleAmount = this.totalAmount;

        // Keep old product_name column populated
        if (this.productName == null || this.productName.isBlank()) {

            if (!items.isEmpty()
                    && items.get(0).getProductName() != null) {

                this.productName =
                        items.get(0).getProductName();
            }
        }
    }

    // =========================================================
// TOTAL PRODUCT QUANTITY
// =========================================================

@Transient
public BigDecimal getTotalProductQuantity() {

    if (items == null || items.isEmpty()) {
        return BigDecimal.ZERO;
    }

    return items.stream()
            .map(SaleItem::getQuantity)
            .filter(Objects::nonNull)
            .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
            );
}


}