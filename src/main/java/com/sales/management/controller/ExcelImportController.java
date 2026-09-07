package com.sales.management.controller;

import com.sales.management.dto.ExcelImportPreviewDTO;
import com.sales.management.dto.ExcelSaleDTO;
import com.sales.management.dto.ExcelSaleItemDTO;
import com.sales.management.model.Sale;
import com.sales.management.model.SaleItem;
import com.sales.management.model.User;
import com.sales.management.repository.SaleRepository;
import com.sales.management.repository.UserRepository;
import com.sales.management.service.ExcelImportService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class ExcelImportController {

    private final ExcelImportService excelImportService;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;

    public ExcelImportController(
            ExcelImportService excelImportService,
            UserRepository userRepository,
            SaleRepository saleRepository) {

        this.excelImportService = excelImportService;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
    }

    // ==========================
    // EXCEL IMPORT PAGE
    // ==========================

    @GetMapping("/excel-import")
    public String excelImportPage(Model model) {

        model.addAttribute(
                "salespersons",
                userRepository.findByRole("SALESPERSON")
        );

        return "excel-import";
    }

    // ==========================
    // UPLOAD + PREVIEW
    // ==========================

    @PostMapping("/excel-import/preview")
    public String previewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("salespersonId") Long salespersonId,
            Model model,
            HttpSession session) {

        try {

            User salesperson =
                    userRepository.findById(salespersonId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Salesperson not found"
                                    )
                            );

            ExcelImportPreviewDTO preview =
                    excelImportService.parseExcel(file);

            // ==========================
            // DUPLICATE CHECK
            // ==========================

            Set<String> duplicateInvoices =
                    new HashSet<>();

            Set<String> fileInvoices =
                    new HashSet<>();

            for (ExcelSaleDTO sale :
                    preview.getSales()) {

                String invoice =
                        sale.getInvoiceNumber();

                if (invoice == null || invoice.isBlank()) {
                    continue;
                }

                if (!fileInvoices.add(invoice)) {
                    duplicateInvoices.add(invoice);
                }

                if (saleRepository.existsByInvoiceNumber(invoice)) {
                    duplicateInvoices.add(invoice);
                }
            }

            model.addAttribute(
                    "preview",
                    preview
            );

            model.addAttribute(
                    "salesperson",
                    salesperson
            );

            model.addAttribute(
                    "salespersonId",
                    salespersonId
            );

            model.addAttribute(
                    "duplicateInvoices",
                    duplicateInvoices
            );

            // Store preview temporarily in session
            session.setAttribute(
                    "excelPreview",
                    preview
            );

            session.setAttribute(
                    "excelSalespersonId",
                    salespersonId
            );

            return "excel-import-preview";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "salespersons",
                    userRepository.findByRole(
                            "SALESPERSON"
                    )
            );

            return "excel-import";
        }
    }

    // ==========================
    // CONFIRM IMPORT
    // ==========================

    @PostMapping("/excel-import/confirm")
    public String confirmImport(
            HttpSession session,
            Model model) {

        try {

            ExcelImportPreviewDTO preview =
                    (ExcelImportPreviewDTO)
                            session.getAttribute(
                                    "excelPreview"
                            );

            Long salespersonId =
                    (Long)
                            session.getAttribute(
                                    "excelSalespersonId"
                            );

            if (preview == null ||
                    salespersonId == null) {

                throw new RuntimeException(
                        "Excel preview expired. Please upload the file again."
                );
            }

            User salesperson =
                    userRepository.findById(
                            salespersonId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Salesperson not found"
                            )
                    );

            // ==========================
            // FINAL DUPLICATE CHECK
            // ==========================

            for (ExcelSaleDTO dto :
                    preview.getSales()) {

                if (dto.getInvoiceNumber() == null ||
                        dto.getInvoiceNumber().isBlank()) {

                    throw new RuntimeException(
                            "Invoice number is missing."
                    );
                }

                if (saleRepository
                        .existsByInvoiceNumber(
                                dto.getInvoiceNumber()
                        )) {

                    throw new RuntimeException(
                            "Invoice already exists: "
                                    + dto.getInvoiceNumber()
                    );
                }
            }

            int importedInvoices = 0;

            // ==========================
            // SAVE SALES
            // ==========================

            for (ExcelSaleDTO dto :
                    preview.getSales()) {

                Sale sale = new Sale();

                sale.setSalesperson(
                        salesperson
                );

                sale.setInvoiceNumber(
                        dto.getInvoiceNumber()
                );

                sale.setSaleDate(
                        dto.getSaleDate()
                );

                sale.setCustomerName(
                        dto.getCustomerName()
                );

                // ==========================
                // LEGACY PRODUCT NAME
                // ==========================

                if (dto.getItems() != null &&
                        !dto.getItems().isEmpty()) {

                    ExcelSaleItemDTO firstItem =
                            dto.getItems().get(0);

                    if (firstItem.getProductName() != null) {

                        sale.setProductName(
                                firstItem.getProductName()
                        );
                    }
                }

                // ==========================
                // SAVE ITEMS
                // ==========================

                if (dto.getItems() != null) {

                    for (ExcelSaleItemDTO itemDTO :
                            dto.getItems()) {

                        SaleItem item =
                                new SaleItem();

                        // Product
                        item.setProductName(
                                itemDTO.getProductName()
                        );

                        // Quantity
                        BigDecimal quantity =
                                itemDTO.getQuantity();

                        if (quantity == null) {
                            quantity = BigDecimal.ZERO;
                        }

                        item.setQuantity(quantity);

                        // Unit
                        String unit =
                                itemDTO.getUnit();

                        if (unit == null) {
                            unit = "";
                        }

                        item.setUnit(unit);

                        // ==========================
                        // PRICE NULL FIX
                        // ==========================

                        BigDecimal price =
                                itemDTO.getPrice();

                        if (price == null) {
                            price = BigDecimal.ZERO;
                        }

                        item.setPrice(price);

                        // ==========================
                        // CALCULATE ITEM AMOUNT
                        // ==========================

                        item.calculateAmount();

                        sale.addItem(item);
                    }
                }

                // ==========================
                // CALCULATE SALE TOTAL
                // ==========================

                sale.calculateTotal();

                // ==========================
                // LEGACY SALE AMOUNT
                // ==========================

                sale.setSaleAmount(
                        sale.getTotalAmount()
                );

                // ==========================
                // SAVE SALE
                // ==========================

                saleRepository.save(sale);

                importedInvoices++;
            }

            // ==========================
            // CLEAR SESSION
            // ==========================

            session.removeAttribute(
                    "excelPreview"
            );

            session.removeAttribute(
                    "excelSalespersonId"
            );

            model.addAttribute(
                    "success",
                    importedInvoices
                            + " invoice(s) imported successfully."
            );

            model.addAttribute(
                    "salespersons",
                    userRepository.findByRole(
                            "SALESPERSON"
                    )
            );

            return "excel-import";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "Import failed: "
                            + e.getMessage()
            );

            model.addAttribute(
                    "salespersons",
                    userRepository.findByRole(
                            "SALESPERSON"
                    )
            );

            return "excel-import";
        }
    }
}