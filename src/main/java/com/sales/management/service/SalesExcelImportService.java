package com.sales.management.service;

import com.sales.management.model.Sale;
import com.sales.management.model.SaleItem;
import com.sales.management.model.User;
import com.sales.management.repository.SaleRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class SalesExcelImportService {

    private final SaleRepository saleRepository;

    public SalesExcelImportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    // =============================================================
    // NORMAL EXCEL IMPORT
    // =============================================================

    public int importSales(
            MultipartFile file,
            User salesperson) throws Exception {

        int importedCount = 0;

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null ||
                    sheet.getPhysicalNumberOfRows() < 2) {

                throw new RuntimeException(
                        "Excel file is empty or contains no sales data.");
            }

            validateHeaders(sheet);

            Map<String, Sale> saleMap =
                    new LinkedHashMap<>();

            /*
             * Excel me same invoice ki next rows me
             * Date / Bill No / Particulars blank ho sakte hain.
             *
             * Example:
             *
             * Row 2 -> 01-08-2026 | 211 | Customer
             * Row 3 -> blank      | blank | blank
             * Row 4 -> blank      | blank | blank
             *
             * Isliye previous values store karenge.
             */
            LocalDate previousDate = null;
            String previousInvoiceNumber = "";
            String previousCustomerName = "";

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;

                try {

                    // =================================================
                    // DATE
                    // =================================================

                    LocalDate saleDate =
                            getLocalDate(row.getCell(0));

                    /*
                     * Agar Date blank hai,
                     * previous row ki date use karo.
                     */
                    if (saleDate == null) {
                        saleDate = previousDate;
                    }

                    if (saleDate == null) {
                        throw new RuntimeException(
                                "Date is required.");
                    }

                    // =================================================
                    // VCH / BILL NO
                    // =================================================

                    String invoiceNumber =
                            getCellString(row.getCell(1)).trim();

                    /*
                     * Agar Bill No blank hai,
                     * previous row ka Bill No use karo.
                     */
                    if (invoiceNumber.isBlank()) {
                        invoiceNumber = previousInvoiceNumber;
                    }

                    if (invoiceNumber.isBlank()) {
                        throw new RuntimeException(
                                "Vch/Bill No is required.");
                    }

                    String invoiceKey =
                            invoiceNumber.trim();

                    // =================================================
                    // PARTICULARS / CUSTOMER
                    // =================================================

                    String customerName =
                            getCellString(row.getCell(2)).trim();

                    /*
                     * Agar Customer blank hai,
                     * previous row ka Customer use karo.
                     */
                    if (customerName.isBlank()) {
                        customerName = previousCustomerName;
                    }

                    if (customerName.isBlank()) {
                        throw new RuntimeException(
                                "Particulars/Customer is required.");
                    }

                    // =================================================
                    // PRODUCT
                    // =================================================

                    String productName =
                            getCellString(row.getCell(3)).trim();

                    if (productName.isBlank()) {
                        throw new RuntimeException(
                                "Item Details/Product is required.");
                    }

                    // =================================================
                    // MATERIAL CENTRE
                    // =================================================

                    String materialCentre =
                            getCellString(row.getCell(4));

                    // Currently not stored in DB.

                    // =================================================
                    // QUANTITY
                    // =================================================

                    BigDecimal quantity =
                            getCellBigDecimal(row.getCell(5));

                    if (quantity == null ||
                            quantity.compareTo(BigDecimal.ZERO) <= 0) {

                        throw new RuntimeException(
                                "Qty. must be greater than zero.");
                    }

                    // =================================================
                    // UNIT
                    // =================================================

                    String unit =
                            getCellString(row.getCell(6)).trim();

                    if (unit.isBlank()) {
                        throw new RuntimeException(
                                "Unit is required.");
                    }

                    // =================================================
                    // PRICE
                    // =================================================

                    BigDecimal price =
                            getCellBigDecimal(row.getCell(7));

                    if (price == null ||
                            price.compareTo(BigDecimal.ZERO) < 0) {

                        throw new RuntimeException(
                                "Price cannot be negative.");
                    }

                    // =================================================
                    // EXCEL AMOUNT
                    // =================================================

                    BigDecimal excelAmount =
                            getCellBigDecimal(row.getCell(8));

                    // =================================================
                    // GET / CREATE SALE
                    // =================================================

                    Sale sale =
                            saleMap.get(invoiceKey);

                    if (sale == null) {

                        /*
                         * Check duplicate invoice in DB.
                         */
                        if (saleRepository
                                .existsByInvoiceNumber(invoiceKey)) {

                            throw new RuntimeException(
                                    "Invoice already exists: "
                                            + invoiceKey);
                        }

                        sale = new Sale();

                        sale.setSalesperson(
                                salesperson);

                        sale.setInvoiceNumber(
                                invoiceKey);

                        sale.setSaleDate(
                                saleDate);

                        sale.setCustomerName(
                                customerName);

                        saleMap.put(
                                invoiceKey,
                                sale);
                    }

                    // =================================================
                    // CREATE SALE ITEM
                    // =================================================

                    SaleItem item =
                            new SaleItem();

                    item.setProductName(
                            productName);

                    item.setQuantity(
                            quantity);

                    item.setUnit(
                            unit);

                    item.setPrice(
                            price);

                    // Qty × Price
                    item.calculateAmount();

                    BigDecimal calculatedAmount =
                            item.getAmount();

                    // =================================================
                    // AMOUNT VALIDATION
                    // =================================================

                    if (excelAmount != null) {

                        BigDecimal difference =
                                calculatedAmount
                                        .subtract(excelAmount)
                                        .abs();

                        if (difference.compareTo(
                                new BigDecimal("0.01")) > 0) {

                            throw new RuntimeException(
                                    "Amount mismatch. Excel Amount="
                                            + excelAmount
                                            + ", Calculated Amount="
                                            + calculatedAmount);
                        }
                    }

                    // =================================================
                    // ADD ITEM
                    // =================================================

                    sale.addItem(item);

                    // =================================================
                    // SAVE CURRENT VALUES FOR NEXT ROW
                    // =================================================

                    previousDate =
                            saleDate;

                    previousInvoiceNumber =
                            invoiceKey;

                    previousCustomerName =
                            customerName;

                } catch (Exception e) {

                    throw new RuntimeException(
                            "Error in Excel row "
                                    + excelRowNumber
                                    + ": "
                                    + e.getMessage());
                }
            }

            // =========================================================
            // SAVE GROUPED SALES
            // =========================================================

            for (Sale sale :
                    saleMap.values()) {

                sale.calculateTotal();

                saleRepository.save(sale);

                importedCount++;
            }
        }

        return importedCount;
    }

    // =============================================================
    // EXCEL PREVIEW
    // =============================================================

    public SalesImportPreview previewSales(
            MultipartFile file,
            User salesperson) throws Exception {

        SalesImportPreview preview =
                new SalesImportPreview();

        preview.setFileName(
                file.getOriginalFilename());

        try (Workbook workbook =
                     WorkbookFactory.create(
                             file.getInputStream())) {

            Sheet sheet =
                    workbook.getSheetAt(0);

            if (sheet == null ||
                    sheet.getPhysicalNumberOfRows() < 2) {

                throw new RuntimeException(
                        "Excel file is empty or contains no sales data.");
            }

            // =========================================================
            // VALIDATE HEADERS
            // =========================================================

            validateHeaders(sheet);

            // =========================================================
            // DUPLICATE CHECK
            // =========================================================

            Set<String> checkedInvoices =
                    new HashSet<>();

            // =========================================================
            // PREVIOUS VALUES
            // =========================================================

            LocalDate previousDate = null;

            String previousInvoiceNumber = "";

            String previousCustomerName = "";

            // =========================================================
            // READ EXCEL ROWS
            // =========================================================

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null ||
                        isEmptyRow(row)) {

                    continue;
                }

                int excelRowNumber =
                        rowIndex + 1;

                try {

                    // =================================================
                    // DATE
                    // =================================================

                    LocalDate saleDate =
                            getLocalDate(
                                    row.getCell(0));

                    /*
                     * Blank date -> previous date
                     */
                    if (saleDate == null) {
                        saleDate =
                                previousDate;
                    }

                    if (saleDate == null) {

                        throw new RuntimeException(
                                "Date is required.");
                    }

                    // =================================================
                    // INVOICE
                    // =================================================

                    String invoiceNumber =
                            getCellString(
                                    row.getCell(1))
                                    .trim();

                    /*
                     * Blank Bill No -> previous Bill No
                     */
                    if (invoiceNumber.isBlank()) {

                        invoiceNumber =
                                previousInvoiceNumber;
                    }

                    if (invoiceNumber.isBlank()) {

                        throw new RuntimeException(
                                "Vch/Bill No is required.");
                    }

                    String invoiceKey =
                            invoiceNumber.trim();

                    // =================================================
                    // CUSTOMER
                    // =================================================

                    String customerName =
                            getCellString(
                                    row.getCell(2))
                                    .trim();

                    /*
                     * Blank customer -> previous customer
                     */
                    if (customerName.isBlank()) {

                        customerName =
                                previousCustomerName;
                    }

                    if (customerName.isBlank()) {

                        throw new RuntimeException(
                                "Particulars/Customer is required.");
                    }

                    // =================================================
                    // PRODUCT
                    // =================================================

                    String productName =
                            getCellString(
                                    row.getCell(3))
                                    .trim();

                    if (productName.isBlank()) {

                        throw new RuntimeException(
                                "Item Details/Product is required.");
                    }

                    // =================================================
                    // MATERIAL CENTRE
                    // =================================================

                    String materialCentre =
                            getCellString(
                                    row.getCell(4));

                    // =================================================
                    // QUANTITY
                    // =================================================

                    BigDecimal quantity =
                            getCellBigDecimal(
                                    row.getCell(5));

                    if (quantity == null ||
                            quantity.compareTo(
                                    BigDecimal.ZERO) <= 0) {

                        throw new RuntimeException(
                                "Qty. must be greater than zero.");
                    }

                    // =================================================
                    // UNIT
                    // =================================================

                    String unit =
                            getCellString(
                                    row.getCell(6))
                                    .trim();

                    if (unit.isBlank()) {

                        throw new RuntimeException(
                                "Unit is required.");
                    }

                    // =================================================
                    // PRICE
                    // =================================================

                    BigDecimal price =
                            getCellBigDecimal(
                                    row.getCell(7));

                    if (price == null ||
                            price.compareTo(
                                    BigDecimal.ZERO) < 0) {

                        throw new RuntimeException(
                                "Price cannot be negative.");
                    }

                    // =================================================
                    // EXCEL AMOUNT
                    // =================================================

                    BigDecimal excelAmount =
                            getCellBigDecimal(
                                    row.getCell(8));

                    // =================================================
                    // CALCULATED AMOUNT
                    // =================================================

                    BigDecimal calculatedAmount =
                            quantity.multiply(price);

                    // =================================================
                    // AMOUNT VALIDATION
                    // =================================================

                    if (excelAmount != null) {

                        BigDecimal difference =
                                calculatedAmount
                                        .subtract(excelAmount)
                                        .abs();

                        if (difference.compareTo(
                                new BigDecimal("0.01")) > 0) {

                            throw new RuntimeException(
                                    "Amount mismatch. Excel Amount="
                                            + excelAmount
                                            + ", Calculated Amount="
                                            + calculatedAmount);
                        }
                    }

                    // =================================================
                    // DATABASE DUPLICATE CHECK
                    // =================================================

                    if (checkedInvoices.add(
                            invoiceKey)) {

                        if (saleRepository
                                .existsByInvoiceNumber(
                                        invoiceKey)) {

                            throw new RuntimeException(
                                    "Invoice already exists: "
                                            + invoiceKey);
                        }
                    }

                    // =================================================
                    // CREATE PREVIEW ROW
                    // =================================================

                    SalesImportPreviewRow previewRow =
                            new SalesImportPreviewRow();

                    previewRow.setRowNumber(
                            excelRowNumber);

                    previewRow.setSaleDate(
                            saleDate);

                    previewRow.setInvoiceNumber(
                            invoiceKey);

                    previewRow.setCustomerName(
                            customerName);

                    previewRow.setProductName(
                            productName);

                    previewRow.setMaterialCentre(
                            materialCentre);

                    previewRow.setQuantity(
                            quantity);

                    previewRow.setUnit(
                            unit);

                    previewRow.setPrice(
                            price);

                    previewRow.setExcelAmount(
                            excelAmount);

                    previewRow.setCalculatedAmount(
                            calculatedAmount);

                    preview.getRows()
                            .add(previewRow);

                    // =================================================
                    // STORE CURRENT VALUES
                    // =================================================

                    previousDate =
                            saleDate;

                    previousInvoiceNumber =
                            invoiceKey;

                    previousCustomerName =
                            customerName;

                } catch (Exception e) {

                    throw new RuntimeException(
                            "Error in Excel row "
                                    + excelRowNumber
                                    + ": "
                                    + e.getMessage());
                }
            }

            // =========================================================
            // NO DATA CHECK
            // =========================================================

            if (preview.getRows().isEmpty()) {

                throw new RuntimeException(
                        "Excel file contains no valid sales data.");
            }
        }

        return preview;
    }

    // =============================================================
    // SAVE PREVIEW DATA
    // =============================================================

    public int savePreview(
            SalesImportPreview preview,
            User salesperson) {

        if (preview == null ||
                preview.getRows() == null ||
                preview.getRows().isEmpty()) {

            throw new RuntimeException(
                    "No preview data available.");
        }

        Map<String, Sale> saleMap =
                new LinkedHashMap<>();

        // =========================================================
        // RE-CHECK DUPLICATES
        // =========================================================

        Set<String> checkedInvoices =
                new HashSet<>();

        // =========================================================
        // CREATE SALES + ITEMS
        // =========================================================

        for (SalesImportPreviewRow row :
                preview.getRows()) {

            String invoiceKey =
                    row.getInvoiceNumber()
                            .trim();

            // =====================================================
            // FINAL DUPLICATE CHECK
            // =====================================================

            if (checkedInvoices.add(
                    invoiceKey)) {

                if (saleRepository
                        .existsByInvoiceNumber(
                                invoiceKey)) {

                    throw new RuntimeException(
                            "Invoice already exists: "
                                    + invoiceKey);
                }
            }

            // =====================================================
            // GET EXISTING SALE FROM CURRENT IMPORT
            // =====================================================

            Sale sale =
                    saleMap.get(invoiceKey);

            // =====================================================
            // CREATE SALE
            // =====================================================

            if (sale == null) {

                sale = new Sale();

                sale.setSalesperson(
                        salesperson);

                sale.setInvoiceNumber(
                        invoiceKey);

                sale.setSaleDate(
                        row.getSaleDate());

                sale.setCustomerName(
                        row.getCustomerName());

                saleMap.put(
                        invoiceKey,
                        sale);
            }

            // =====================================================
            // CREATE SALE ITEM
            // =====================================================

            SaleItem item =
                    new SaleItem();

            item.setProductName(
                    row.getProductName());

            item.setQuantity(
                    row.getQuantity());

            item.setUnit(
                    row.getUnit());

            item.setPrice(
                    row.getPrice());

            item.calculateAmount();

            // =====================================================
            // ADD ITEM
            // =====================================================

            sale.addItem(item);
        }

        // =========================================================
        // SAVE GROUPED SALES
        // =========================================================

        int importedCount = 0;

        for (Sale sale :
                saleMap.values()) {

            sale.calculateTotal();

            saleRepository.save(sale);

            importedCount++;
        }

        return importedCount;
    }

    // =============================================================
    // VALIDATE EXCEL HEADERS
    // =============================================================

    private void validateHeaders(
            Sheet sheet) {

        Row headerRow =
                sheet.getRow(0);

        if (headerRow == null) {

            throw new RuntimeException(
                    "Excel header row is missing.");
        }

        String[] expectedHeaders = {

                "Date",
                "Vch/Bill No",
                "Particulars",
                "Item Details",
                "Material Centre",
                "Qty.",
                "Unit",
                "Price",
                "Amount"
        };

        for (int i = 0;
             i < expectedHeaders.length;
             i++) {

            String actualHeader =
                    getCellString(
                            headerRow.getCell(i));

            if (!normalize(actualHeader)
                    .equals(
                            normalize(
                                    expectedHeaders[i]))) {

                throw new RuntimeException(
                        "Invalid Excel header at column "
                                + (i + 1)
                                + ". Expected: "
                                + expectedHeaders[i]
                                + ", Found: "
                                + actualHeader);
            }
        }
    }

    // =============================================================
    // GET STRING
    // =============================================================

    private String getCellString(
            Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter =
                new DataFormatter();

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    // =============================================================
    // GET BIG DECIMAL
    // =============================================================

    private BigDecimal getCellBigDecimal(
            Cell cell) {

        if (cell == null) {
            return null;
        }

        try {

            if (cell.getCellType()
                    == CellType.NUMERIC) {

                return BigDecimal.valueOf(
                        cell.getNumericCellValue());
            }

            String value =
                    getCellString(cell);

            if (value.isBlank()) {
                return null;
            }

            /*
             * Remove commas.
             *
             * Example:
             * 1,500.00 -> 1500.00
             */
            value =
                    value.replace(",", "");

            return new BigDecimal(value);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid numeric value: "
                            + getCellString(cell));
        }
    }

    // =============================================================
    // GET DATE
    // =============================================================

    private LocalDate getLocalDate(
            Cell cell) {

        if (cell == null) {
            return null;
        }

        try {

            // =====================================================
            // EXCEL DATE CELL
            // =====================================================

            if (cell.getCellType()
                    == CellType.NUMERIC) {

                /*
                 * Normal Excel date
                 */
                if (DateUtil.isCellDateFormatted(cell)) {

                    return cell
                            .getDateCellValue()
                            .toInstant()
                            .atZone(
                                    ZoneId.systemDefault())
                            .toLocalDate();
                }

                /*
                 * Excel serial date fallback.
                 *
                 * This handles numeric dates even when
                 * Excel formatting is not detected.
                 */
                double numericValue =
                        cell.getNumericCellValue();

                if (numericValue > 0 &&
                        numericValue < 100000) {

                    try {

                        return DateUtil
                                .getJavaDate(
                                        numericValue)
                                .toInstant()
                                .atZone(
                                        ZoneId.systemDefault())
                                .toLocalDate();

                    } catch (Exception ignored) {
                        // Continue to string parsing
                    }
                }
            }

            // =====================================================
            // STRING DATE
            // =====================================================

            String value =
                    getCellString(cell);

            if (value.isBlank()) {
                return null;
            }

            /*
             * Supported formats
             */
            List<DateTimeFormatter> formatters =
                    Arrays.asList(

                            DateTimeFormatter.ofPattern(
                                    "dd-MM-yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd"),

                            DateTimeFormatter.ofPattern(
                                    "dd.MM.yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "d-M-yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "d/M/yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "d.M.yyyy"),

                            DateTimeFormatter.ofPattern(
                                    "dd-MM-yy"),

                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yy")
                    );

            for (DateTimeFormatter formatter :
                    formatters) {

                try {

                    return LocalDate.parse(
                            value,
                            formatter);

                } catch (DateTimeParseException ignored) {

                    // Try next format
                }
            }

            /*
             * Some Excel exports may contain date-time.
             */
            String[] dateTimePatterns = {

                    "dd-MM-yyyy HH:mm:ss",
                    "dd/MM/yyyy HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "dd-MM-yyyy HH:mm",
                    "dd/MM/yyyy HH:mm"
            };

            for (String pattern :
                    dateTimePatterns) {

                try {

                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern(
                                    pattern);

                    return java.time.LocalDateTime
                            .parse(
                                    value,
                                    formatter)
                            .toLocalDate();

                } catch (DateTimeParseException ignored) {

                    // Try next pattern
                }
            }

            throw new RuntimeException(
                    "Invalid date format: "
                            + value);

        } catch (Exception e) {

            /*
             * Blank date is handled above.
             * If parsing failed, provide useful error.
             */
            String cellValue =
                    getCellString(cell);

            if (cellValue.isBlank()) {
                return null;
            }

            throw new RuntimeException(
                    "Invalid date: "
                            + cellValue);
        }
    }

    // =============================================================
    // NORMALIZE HEADER
    // =============================================================

    private String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll(
                        "\\s+",
                        " ")
                .toLowerCase();
    }

    // =============================================================
    // EMPTY ROW
    // =============================================================

    private boolean isEmptyRow(
            Row row) {

        for (int i = 0;
             i < 9;
             i++) {

            Cell cell =
                    row.getCell(i);

            if (cell != null &&
                    !getCellString(cell)
                            .isBlank()) {

                return false;
            }
        }

        return true;
    }

    // =============================================================
    // PREVIEW DTO
    // =============================================================

    public static class SalesImportPreview {

        private String fileName;

        private List<SalesImportPreviewRow> rows =
                new ArrayList<>();

        public String getFileName() {
            return fileName;
        }

        public void setFileName(
                String fileName) {

            this.fileName = fileName;
        }

        public List<SalesImportPreviewRow> getRows() {
            return rows;
        }

        public void setRows(
                List<SalesImportPreviewRow> rows) {

            this.rows = rows;
        }

        public int getTotalRows() {

            return rows != null
                    ? rows.size()
                    : 0;
        }

        public BigDecimal getTotalQuantity() {

            if (rows == null) {
                return BigDecimal.ZERO;
            }

            return rows.stream()

                    .map(
                            SalesImportPreviewRow
                                    ::getQuantity)

                    .filter(
                            Objects::nonNull)

                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add);
        }

        public BigDecimal getTotalAmount() {

            if (rows == null) {
                return BigDecimal.ZERO;
            }

            return rows.stream()

                    .map(
                            SalesImportPreviewRow
                                    ::getCalculatedAmount)

                    .filter(
                            Objects::nonNull)

                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add);
        }
    }

    // =============================================================
    // PREVIEW ROW DTO
    // =============================================================

    public static class SalesImportPreviewRow {

        private int rowNumber;

        private LocalDate saleDate;

        private String invoiceNumber;

        private String customerName;

        private String productName;

        private String materialCentre;

        private BigDecimal quantity;

        private String unit;

        private BigDecimal price;

        private BigDecimal excelAmount;

        private BigDecimal calculatedAmount;

        // =========================================================
        // ROW NUMBER
        // =========================================================

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(
                int rowNumber) {

            this.rowNumber = rowNumber;
        }

        // =========================================================
        // DATE
        // =========================================================

        public LocalDate getSaleDate() {
            return saleDate;
        }

        public void setSaleDate(
                LocalDate saleDate) {

            this.saleDate = saleDate;
        }

        // =========================================================
        // INVOICE
        // =========================================================

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public void setInvoiceNumber(
                String invoiceNumber) {

            this.invoiceNumber =
                    invoiceNumber;
        }

        // =========================================================
        // CUSTOMER
        // =========================================================

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(
                String customerName) {

            this.customerName =
                    customerName;
        }

        // =========================================================
        // PRODUCT
        // =========================================================

        public String getProductName() {
            return productName;
        }

        public void setProductName(
                String productName) {

            this.productName =
                    productName;
        }

        // =========================================================
        // MATERIAL CENTRE
        // =========================================================

        public String getMaterialCentre() {
            return materialCentre;
        }

        public void setMaterialCentre(
                String materialCentre) {

            this.materialCentre =
                    materialCentre;
        }

        // =========================================================
        // QUANTITY
        // =========================================================

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(
                BigDecimal quantity) {

            this.quantity = quantity;
        }

        // =========================================================
        // UNIT
        // =========================================================

        public String getUnit() {
            return unit;
        }

        public void setUnit(
                String unit) {

            this.unit = unit;
        }

        // =========================================================
        // PRICE
        // =========================================================

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(
                BigDecimal price) {

            this.price = price;
        }

        // =========================================================
        // EXCEL AMOUNT
        // =========================================================

        public BigDecimal getExcelAmount() {
            return excelAmount;
        }

        public void setExcelAmount(
                BigDecimal excelAmount) {

            this.excelAmount =
                    excelAmount;
        }

        // =========================================================
        // CALCULATED AMOUNT
        // =========================================================

        public BigDecimal getCalculatedAmount() {
            return calculatedAmount;
        }

        public void setCalculatedAmount(
                BigDecimal calculatedAmount) {

            this.calculatedAmount =
                    calculatedAmount;
        }
    }
}