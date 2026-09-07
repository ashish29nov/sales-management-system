package com.sales.management.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExcelImportPreviewDTO {

    private List<ExcelSaleDTO> sales = new ArrayList<>();

    private int totalInvoices;

    private int totalItems;

    private BigDecimal grandTotal = BigDecimal.ZERO;

    public ExcelImportPreviewDTO() {
    }

    public List<ExcelSaleDTO> getSales() {
        return sales;
    }

    public void setSales(List<ExcelSaleDTO> sales) {
        this.sales = sales;
    }

    public int getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(int totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public void calculateSummary() {

        totalInvoices = sales.size();

        totalItems = sales.stream()
                .mapToInt(
                        sale -> sale.getItems().size()
                )
                .sum();

        grandTotal = sales.stream()
                .map(ExcelSaleDTO::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}