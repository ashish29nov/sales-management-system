package com.sales.management.service;

import com.sales.management.dto.ExcelImportPreviewDTO;
import com.sales.management.dto.ExcelSaleDTO;
import com.sales.management.dto.ExcelSaleItemDTO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ExcelImportService {

    // ==========================
    // PARSE EXCEL FILE
    // ==========================

    public ExcelImportPreviewDTO parseExcel(
            MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select an Excel file."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                (!fileName.toLowerCase().endsWith(".xlsx")
                        &&
                 !fileName.toLowerCase().endsWith(".xls"))) {

            throw new IllegalArgumentException(
                    "Only .xlsx and .xls files are allowed."
            );
        }

        try (InputStream inputStream =
                     file.getInputStream();
             Workbook workbook =
                     WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {

                throw new IllegalArgumentException(
                        "Excel file does not contain any sheet."
                );
            }

            Sheet sheet =
                    workbook.getSheetAt(0);

            return parseSheet(sheet);
        }
    }

    // ==========================
    // PARSE SHEET
    // ==========================

    private ExcelImportPreviewDTO parseSheet(
            Sheet sheet) {

        ExcelImportPreviewDTO preview =
                new ExcelImportPreviewDTO();

        if (sheet == null ||
                sheet.getPhysicalNumberOfRows() == 0) {

            throw new IllegalArgumentException(
                    "Excel sheet is empty."
            );
        }

        // ==========================
        // FIND HEADER ROW
        // ==========================

        Row headerRow = null;

        for (int i = sheet.getFirstRowNum();
             i <= sheet.getLastRowNum();
             i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Map<String, Integer> testColumns =
                    detectColumns(row);

            if (testColumns.containsKey("date")
                    && testColumns.containsKey("invoice")
                    && testColumns.containsKey("product")) {

                headerRow = row;
                break;
            }
        }

        if (headerRow == null) {

            throw new IllegalArgumentException(
                    "Excel header row not found."
            );
        }

        Map<String, Integer> columns =
                detectColumns(headerRow);

        validateColumns(columns);

        Map<String, ExcelSaleDTO> invoiceMap =
                new LinkedHashMap<>();

        String lastDate = "";
        String lastInvoice = "";
        String lastCustomer = "";

        DataFormatter formatter =
                new DataFormatter();

        // ==========================
        // READ DATA ROWS
        // ==========================

        for (int rowIndex =
                     headerRow.getRowNum() + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row =
                    sheet.getRow(rowIndex);

            if (row == null ||
                    isRowEmpty(row)) {

                continue;
            }

            // ==========================
            // DATE
            // ==========================

            String dateText =
                    getCellValue(
                            row,
                            columns.get("date"),
                            formatter
                    );

            if (!dateText.isBlank()) {

                lastDate = dateText;
            }

            // ==========================
            // INVOICE
            // ==========================

            String invoiceNumber =
                    getCellValue(
                            row,
                            columns.get("invoice"),
                            formatter
                    );

            if (!invoiceNumber.isBlank()) {

                lastInvoice = invoiceNumber;
            }

            // ==========================
            // CUSTOMER
            // ==========================

            String customerName =
                    getCellValue(
                            row,
                            columns.get("customer"),
                            formatter
                    );

            if (!customerName.isBlank()) {

                lastCustomer = customerName;
            }

            if (lastInvoice.isBlank()) {
                continue;
            }

            // ==========================
            // CREATE / GET SALE
            // ==========================

            ExcelSaleDTO sale =
                    invoiceMap.get(lastInvoice);

            if (sale == null) {

                sale = new ExcelSaleDTO();

                sale.setInvoiceNumber(
                        lastInvoice
                );

                sale.setSaleDate(
                        parseDate(lastDate, row,
                                columns.get("date"))
                );

                sale.setCustomerName(
                        lastCustomer
                );

                invoiceMap.put(
                        lastInvoice,
                        sale
                );
            }

            // ==========================
            // PRODUCT
            // ==========================

            String productName =
                    getCellValue(
                            row,
                            columns.get("product"),
                            formatter
                    );

            if (productName.isBlank()) {

                continue;
            }

            // ==========================
            // QUANTITY
            // ==========================

            BigDecimal quantity =
                    getBigDecimal(
                            row,
                            columns.get("quantity"),
                            formatter
                    );

            if (quantity == null) {

                quantity = BigDecimal.ZERO;
            }

            // ==========================
            // UNIT
            // ==========================

            String unit =
                    getCellValue(
                            row,
                            columns.get("unit"),
                            formatter
                    );

            if (unit == null) {
                unit = "";
            }

            // ==========================
            // PRICE
            // ==========================

            BigDecimal price =
                    getBigDecimal(
                            row,
                            columns.get("price"),
                            formatter
                    );

            // IMPORTANT:
            // Database price is NOT NULL
            if (price == null) {

                price = BigDecimal.ZERO;
            }

            // ==========================
            // AMOUNT
            // ==========================

            BigDecimal amount =
                    getBigDecimal(
                            row,
                            columns.get("amount"),
                            formatter
                    );

            // If amount missing, calculate
            if (amount == null) {

                amount =
                        quantity.multiply(price);
            }

            // ==========================
            // CREATE ITEM DTO
            // ==========================

            ExcelSaleItemDTO item =
                    new ExcelSaleItemDTO();

            item.setProductName(
                    productName
            );

            item.setQuantity(
                    quantity
            );

            item.setUnit(
                    unit
            );

            item.setPrice(
                    price
            );

            item.setAmount(
                    amount
            );

            sale.getItems().add(item);
        }

        // ==========================
        // CALCULATE SALE TOTALS
        // ==========================

        for (ExcelSaleDTO sale :
                invoiceMap.values()) {

            sale.calculateTotal();

            preview.getSales().add(
                    sale
            );
        }

        // ==========================
        // SUMMARY
        // ==========================

        preview.calculateSummary();

        return preview;
    }

    // ==========================
    // DETECT COLUMNS
    // ==========================

    private Map<String, Integer> detectColumns(
            Row headerRow) {

        Map<String, Integer> columns =
                new HashMap<>();

        DataFormatter formatter =
                new DataFormatter();

        for (int i = 0;
             i < headerRow.getLastCellNum();
             i++) {

            Cell cell =
                    headerRow.getCell(i);

            if (cell == null) {
                continue;
            }

            String header =
                    formatter
                            .formatCellValue(cell)
                            .trim()
                            .toLowerCase();

            header =
                    header.replace(
                            "\n",
                            " "
                    );

            header =
                    header.replace(
                            "_",
                            " "
                    );

            // DATE
            if (header.contains("date")) {

                columns.put(
                        "date",
                        i
                );

            // INVOICE
            } else if (
                    header.contains("vch")
                            ||
                    header.contains("bill")
                            ||
                    header.contains("invoice")) {

                columns.put(
                        "invoice",
                        i
                );

            // CUSTOMER
            } else if (
                    header.contains("particular")) {

                columns.put(
                        "customer",
                        i
                );

            // PRODUCT
            } else if (
                    header.contains("item")
                            ||
                    header.contains("product")) {

                columns.put(
                        "product",
                        i
                );

            // QUANTITY
           } else if (
        header.equals("qty")
                ||
        header.equals("qty.")
                ||
        header.equals("quantity")
                ||
        header.startsWith("qty ")
                ||
        header.startsWith("quantity ")
                ||
        header.contains("quantity")) {

    columns.put(
            "quantity",
            i
    );

            // UNIT
            } else if (
                    header.equals("unit")
                            ||
                    header.contains("unit")) {

                columns.put(
                        "unit",
                        i
                );

            // PRICE
            } else if (
                    header.equals("price")
                            ||
                    header.contains("rate")
                            ||
                    header.contains("selling price")) {

                columns.put(
                        "price",
                        i
                );

            // AMOUNT
            } else if (
                    header.equals("amount")
                            ||
                    header.contains("value")
                            ||
                    header.contains("total")) {

                columns.put(
                        "amount",
                        i
                );
            }
        }

        return columns;
    }

    // ==========================
    // VALIDATE COLUMNS
    // ==========================

    private void validateColumns(
            Map<String, Integer> columns) {

        String[] required = {
                "date",
                "invoice",
                "customer",
                "product",
                "quantity",
                "unit",
                "price",
                "amount"
        };

        List<String> missing =
                new ArrayList<>();

        for (String column :
                required) {

            if (!columns.containsKey(column)) {

                missing.add(column);
            }
        }

        if (!missing.isEmpty()) {

            throw new IllegalArgumentException(
                    "Missing Excel columns: "
                            + String.join(
                                    ", ",
                                    missing
                            )
            );
        }
    }

    // ==========================
    // GET CELL VALUE
    // ==========================

    private String getCellValue(
            Row row,
            Integer columnIndex,
            DataFormatter formatter) {

        if (columnIndex == null) {
            return "";
        }

        Cell cell =
                row.getCell(columnIndex);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    // ==========================
// BIG DECIMAL
// ==========================

private BigDecimal getBigDecimal(
        Row row,
        Integer columnIndex,
        DataFormatter formatter) {

    if (columnIndex == null) {
        return null;
    }

    Cell cell = row.getCell(columnIndex);

    if (cell == null) {
        return null;
    }

    try {

        // =========================================
        // NUMERIC CELL
        // =========================================

        if (cell.getCellType() == CellType.NUMERIC) {

            return BigDecimal.valueOf(
                    cell.getNumericCellValue()
            );
        }

        // =========================================
        // FORMULA CELL
        // =========================================

        if (cell.getCellType() == CellType.FORMULA) {

            try {

                double numericValue =
                        cell.getNumericCellValue();

                return BigDecimal.valueOf(
                        numericValue
                );

            } catch (Exception ignored) {
                // Continue with formatted value
            }
        }

        // =========================================
        // STRING / FORMULA TEXT VALUE
        // =========================================

        String value =
                formatter
                        .formatCellValue(cell)
                        .trim();

        if (value.isBlank()) {
            return null;
        }

        // =========================================
        // CLEAN VALUE
        // =========================================

        value = value
                .replace(",", "")
                .replace("₹", "")
                .replace("$", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace("\u00A0", " ")
                .trim();

        // =========================================
        // HANDLE COMMON UNIT TEXT
        // Example:
        // 10 PCS
        // 10.000 PCS
        // 25 KG
        // =========================================

        value = value.replaceAll(
                "(?i)\\s*(pcs|pc|nos|no|kg|kgs|gm|g|ltr|l|meter|m)\\s*$",
                ""
        ).trim();

        if (value.isBlank()) {
            return null;
        }

        // =========================================
        // NORMAL DECIMAL
        // =========================================

        return new BigDecimal(value);

    } catch (Exception e) {

        System.out.println(
                "Unable to parse numeric Excel value: "
                        + formatter.formatCellValue(cell)
                        + " | column: "
                        + columnIndex
        );

        return null;
    }
}

    // ==========================
    // DATE PARSING
    // ==========================

    private LocalDate parseDate(
            String value,
            Row row,
            Integer columnIndex) {

        // First try actual Excel date cell
        if (columnIndex != null) {

            Cell cell =
                    row.getCell(columnIndex);

            if (cell != null &&
                    cell.getCellType()
                            == CellType.NUMERIC &&
                    DateUtil.isCellDateFormatted(cell)) {

                return cell
                        .getDateCellValue()
                        .toInstant()
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .toLocalDate();
            }
        }

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        // Remove time if present
        if (value.contains(" ")) {

            value =
                    value.substring(
                            0,
                            value.indexOf(" ")
                    );
        }

        List<DateTimeFormatter> formats =
                Arrays.asList(

                        DateTimeFormatter.ofPattern(
                                "dd-MM-yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "d-M-yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "d/M/yyyy"
                        ),

                        DateTimeFormatter.ISO_LOCAL_DATE
                );

        for (DateTimeFormatter format :
                formats) {

            try {

                return LocalDate.parse(
                        value,
                        format
                );

            } catch (
                    DateTimeParseException ignored) {
            }
        }

        // Excel serial date as text
        try {

            double serial =
                    Double.parseDouble(value);

            return DateUtil
                    .getLocalDateTime(
                            serial
                    )
                    .toLocalDate();

        } catch (Exception ignored) {
        }

        return null;
    }

    // ==========================
    // CHECK EMPTY ROW
    // ==========================

    private boolean isRowEmpty(
            Row row) {

        DataFormatter formatter =
                new DataFormatter();

        for (int i = 0;
             i < row.getLastCellNum();
             i++) {

            Cell cell =
                    row.getCell(i);

            if (cell != null &&
                    !formatter
                            .formatCellValue(cell)
                            .trim()
                            .isEmpty()) {

                return false;
            }
        }

        return true;
    }
}