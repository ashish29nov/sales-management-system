package com.sales.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExcelSaleDTO {

    private String invoiceNumber;

    private LocalDate saleDate;

    private String customerName;

    private List<ExcelSaleItemDTO> items = new ArrayList<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;

    public ExcelSaleDTO() {
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

    public List<ExcelSaleItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ExcelSaleItemDTO> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void calculateTotal() {

        totalAmount = items.stream()
                .map(ExcelSaleItemDTO::getAmount)
                .filter(amount -> amount != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}