package com.sales.management.controller;

import com.sales.management.model.Client;
import com.sales.management.model.Sale;
import com.sales.management.model.SaleItem;
import com.sales.management.model.Target;
import com.sales.management.model.User;
import com.sales.management.repository.ClientRepository;
import com.sales.management.repository.SaleRepository;
import com.sales.management.repository.TargetRepository;
import com.sales.management.repository.UserRepository;
import com.sales.management.service.SalesExcelImportService;
import com.sales.management.service.SalesExcelImportService.SalesImportPreview;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalespersonController {


    private final UserRepository userRepository;
    private final TargetRepository targetRepository;
    private final SaleRepository saleRepository;
    private final SalesExcelImportService salesExcelImportService;
    private final ClientRepository clientRepository;

    public SalespersonController(
        UserRepository userRepository,
        TargetRepository targetRepository,
        SaleRepository saleRepository,
        ClientRepository clientRepository,
        SalesExcelImportService salesExcelImportService) {

    this.userRepository = userRepository;
    this.targetRepository = targetRepository;
    this.saleRepository = saleRepository;
    this.clientRepository = clientRepository;
    this.salesExcelImportService = salesExcelImportService;
}

    // =========================================================
    // SALESPERSON DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(name = "month", required = false) String month,
            Authentication authentication,
            Model model) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        YearMonth selectedMonth =
                getSelectedMonth(month);

        String selectedMonthString =
                selectedMonth.toString();

        LocalDate startDate =
                selectedMonth.atDay(1);

        LocalDate endDate =
                selectedMonth.atEndOfMonth();

        // =====================================================
        // TARGET
        // =====================================================

        Target target =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString)
                        .orElse(null);

        BigDecimal targetAmount =
                BigDecimal.ZERO;

        if (target != null &&
                target.getTargetAmount() != null) {

            targetAmount =
                    target.getTargetAmount();
        }

        // =====================================================
        // ACHIEVED SALES
        // =====================================================

        BigDecimal achievedAmount =
                saleRepository.getTotalSalesForPeriod(
                        salesperson,
                        startDate,
                        endDate);

        if (achievedAmount == null) {
            achievedAmount =
                    BigDecimal.ZERO;
        }

        // =====================================================
        // TOTAL PRODUCT QUANTITY
        // =====================================================

        BigDecimal totalProductQuantity =
                saleRepository
                        .getTotalProductQuantityForPeriod(
                                salesperson,
                                startDate,
                                endDate);

        if (totalProductQuantity == null) {
            totalProductQuantity =
                    BigDecimal.ZERO;
        }

        // =====================================================
        // REMAINING
        // =====================================================

        BigDecimal remainingAmount =
                targetAmount.subtract(
                        achievedAmount);

        if (remainingAmount.compareTo(
                BigDecimal.ZERO) < 0) {

            remainingAmount =
                    BigDecimal.ZERO;
        }

        // =====================================================
        // ACHIEVEMENT %
        // =====================================================

        BigDecimal achievementPercentage =
                calculatePercentage(
                        achievedAmount,
                        targetAmount);

        // =====================================================
        // PROGRESS BAR %
        // =====================================================

        BigDecimal progressPercentage =
                achievementPercentage;

        if (progressPercentage.compareTo(
                BigDecimal.valueOf(100)) > 0) {

            progressPercentage =
                    BigDecimal.valueOf(100);
        }

        // =====================================================
        // RECENT SALES
        // =====================================================

        List<Sale> recentSales =
                saleRepository
                        .findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
                                salesperson,
                                startDate,
                                endDate);

        if (recentSales.size() > 5) {

            recentSales =
                    recentSales.subList(0, 5);
        }

        // Initialize lazy items
        recentSales.forEach(
                sale -> sale.getItems().size()
        );

        // =====================================================
        // MODEL
        // =====================================================

        model.addAttribute(
                "salesperson",
                salesperson);

        model.addAttribute(
                "targetAmount",
                targetAmount);

        model.addAttribute(
                "achievedAmount",
                achievedAmount);

        model.addAttribute(
                "remainingAmount",
                remainingAmount);

        model.addAttribute(
                "achievementPercentage",
                achievementPercentage);

        model.addAttribute(
                "progressPercentage",
                progressPercentage);

        model.addAttribute(
                "selectedMonth",
                selectedMonthString);

        model.addAttribute(
                "recentSales",
                recentSales);

        model.addAttribute(
                "totalProductQuantity",
                totalProductQuantity);

        return "sales-dashboard";
    }

    // =========================================================
    // MY SALES
    // =========================================================

    @GetMapping("/my-sales")
public String mySales(

        @RequestParam(
                name = "month",
                required = false)
        String month,

        @RequestParam(
                name = "customer",
                required = false)
        String customer,

        @RequestParam(
                name = "startDate",
                required = false)
        String startDate,

        @RequestParam(
                name = "endDate",
                required = false)
        String endDate,

        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    LocalDate fromDate = null;
    LocalDate toDate = null;

    // =====================================================
    // CURRENT MONTH / SELECTED MONTH
    // =====================================================

    if (month == null || month.isBlank()) {

        // Current month automatically select
        month = YearMonth.now().toString();
    }

    if (month != null && !month.isBlank()) {

        YearMonth yearMonth =
                YearMonth.parse(month);

        fromDate =
                yearMonth.atDay(1);

        toDate =
                yearMonth.atEndOfMonth();
    }

    // =====================================================
    // START DATE / END DATE
    // =====================================================
    // Agar Date filter diya gaya hai to usko priority milegi

    if (startDate != null &&
            !startDate.isBlank()) {

        fromDate =
                LocalDate.parse(startDate);
    }

    if (endDate != null &&
            !endDate.isBlank()) {

        toDate =
                LocalDate.parse(endDate);
    }

    // =====================================================
    // GET FILTERED SALES
    // =====================================================

    List<Sale> sales =
            saleRepository.findMySalesWithFilters(
                    salesperson,
                    customer,
                    fromDate,
                    toDate
            );

    // =====================================================
    // INITIALIZE LAZY ITEMS
    // =====================================================

    sales.forEach(
            sale -> sale.getItems().size()
    );

    // =====================================================
    // MODEL
    // =====================================================

    model.addAttribute(
            "salesperson",
            salesperson
    );

    model.addAttribute(
            "sales",
            sales
    );

    model.addAttribute(
            "month",
            month
    );

    model.addAttribute(
            "customer",
            customer != null
                    ? customer
                    : ""
    );

    model.addAttribute(
            "startDate",
            startDate != null
                    ? startDate
                    : ""
    );

    model.addAttribute(
            "endDate",
            endDate != null
                    ? endDate
                    : ""
    );

    // =====================================================
    // RETURN PAGE
    // =====================================================

    return "my-sales";
}

    // =========================================================
    // MY TARGET
    // =========================================================

    @GetMapping("/my-target")
public String myTarget(
        @RequestParam(
                name = "month",
                required = false)
        String month,

        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    YearMonth selectedMonth =
            getSelectedMonth(month);

    String selectedMonthString =
            selectedMonth.toString();

    LocalDate startDate =
            selectedMonth.atDay(1);

    LocalDate endDate =
            selectedMonth.atEndOfMonth();


    // =====================================================
    // TARGET
    // =====================================================

    Target target =
            targetRepository
                    .findBySalespersonAndTargetMonth(
                            salesperson,
                            selectedMonthString)
                    .orElse(null);


    // =====================================================
    // SALES TARGET
    // =====================================================

    BigDecimal targetAmount =
            target != null
                    ? target.getTargetAmount()
                    : BigDecimal.ZERO;


    // =====================================================
    // SALES ACHIEVED
    // =====================================================

    BigDecimal achievedAmount =
            saleRepository.getTotalSalesForPeriod(
                    salesperson,
                    startDate,
                    endDate);

    if (achievedAmount == null) {

        achievedAmount =
                BigDecimal.ZERO;
    }


    // =====================================================
    // SALES REMAINING
    // =====================================================

    BigDecimal remainingAmount =
            targetAmount.subtract(
                    achievedAmount);

    if (remainingAmount.compareTo(
            BigDecimal.ZERO) < 0) {

        remainingAmount =
                BigDecimal.ZERO;
    }


    // =====================================================
    // SALES ACHIEVEMENT %
    // =====================================================

    BigDecimal achievementPercentage =
            calculatePercentage(
                    achievedAmount,
                    targetAmount);


    // =====================================================
    // SALES PROGRESS %
    // =====================================================

    BigDecimal progressPercentage =
            achievementPercentage;

    if (progressPercentage.compareTo(
            BigDecimal.valueOf(100)) > 0) {

        progressPercentage =
                BigDecimal.valueOf(100);
    }


    // =====================================================
    // CLIENT TARGET
    // =====================================================

    Integer clientTarget =
            target != null
                    && target.getClientTarget() != null
                    ? target.getClientTarget()
                    : 0;


    // =====================================================
    // ACTUAL NEW CLIENTS
    // =====================================================

    long actualClients =
            clientRepository.countNewClientsForPeriod(
                    salesperson,
                    startDate,
                    endDate);


    // =====================================================
    // CLIENT REMAINING
    // =====================================================

    long remainingClients =
            clientTarget - actualClients;

    if (remainingClients < 0) {

        remainingClients = 0;
    }


    // =====================================================
    // CLIENT ACHIEVEMENT %
    // =====================================================

    BigDecimal clientAchievementPercentage =
            BigDecimal.ZERO;

    if (clientTarget > 0) {

        clientAchievementPercentage =
                BigDecimal.valueOf(actualClients)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(clientTarget),
                                2,
                                java.math.RoundingMode.HALF_UP);
    }


    // =====================================================
    // CLIENT PROGRESS %
    // =====================================================

    BigDecimal clientProgressPercentage =
            clientAchievementPercentage;

    if (clientProgressPercentage.compareTo(
            BigDecimal.valueOf(100)) > 0) {

        clientProgressPercentage =
                BigDecimal.valueOf(100);
    }


    // =====================================================
    // MODEL
    // =====================================================

    model.addAttribute(
            "salesperson",
            salesperson);

    model.addAttribute(
            "selectedMonth",
            selectedMonthString);


    // ---------------- SALES ----------------

    model.addAttribute(
            "targetAmount",
            targetAmount);

    model.addAttribute(
            "achievedAmount",
            achievedAmount);

    model.addAttribute(
            "remainingAmount",
            remainingAmount);

    model.addAttribute(
            "achievementPercentage",
            achievementPercentage);

    model.addAttribute(
            "progressPercentage",
            progressPercentage);


    // ---------------- CLIENTS ----------------

    model.addAttribute(
            "clientTarget",
            clientTarget);

    model.addAttribute(
            "actualClients",
            actualClients);

    model.addAttribute(
            "remainingClients",
            remainingClients);

    model.addAttribute(
            "clientAchievementPercentage",
            clientAchievementPercentage);

    model.addAttribute(
            "clientProgressPercentage",
            clientProgressPercentage);


    return "my-target";
}

    // =========================================================
    // MY REPORT
    // =========================================================

    @GetMapping("/my-report")
public String myReport(
        @RequestParam(
                name = "month",
                required = false)
        String month,

        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    YearMonth selectedMonth =
            getSelectedMonth(month);

    String selectedMonthString =
            selectedMonth.toString();

    LocalDate startDate =
            selectedMonth.atDay(1);

    LocalDate endDate =
            selectedMonth.atEndOfMonth();


    // =====================================================
    // TARGET
    // =====================================================

    Target target =
            targetRepository
                    .findBySalespersonAndTargetMonth(
                            salesperson,
                            selectedMonthString)
                    .orElse(null);


    BigDecimal targetAmount =
            target != null
                    ? target.getTargetAmount()
                    : BigDecimal.ZERO;


    // =====================================================
    // MONTHLY SALES
    // =====================================================

    List<Sale> monthlySales =
            saleRepository
                    .findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
                            salesperson,
                            startDate,
                            endDate);


    // =====================================================
    // ACHIEVED
    // =====================================================

    BigDecimal achievedAmount =
            monthlySales
                    .stream()
                    .map(Sale::getTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add);


    // =====================================================
    // REMAINING
    // =====================================================

    BigDecimal remainingAmount =
            targetAmount.subtract(
                    achievedAmount);

    if (remainingAmount.compareTo(
            BigDecimal.ZERO) < 0) {

        remainingAmount =
                BigDecimal.ZERO;
    }


    // =====================================================
    // ACHIEVEMENT %
    // =====================================================

    BigDecimal achievementPercentage =
            calculatePercentage(
                    achievedAmount,
                    targetAmount);


    // =====================================================
    // PROGRESS BAR %
    // =====================================================

    BigDecimal progressPercentage =
            achievementPercentage;

    if (progressPercentage.compareTo(
            BigDecimal.valueOf(100)) > 0) {

        progressPercentage =
                BigDecimal.valueOf(100);
    }


    // =====================================================
    // PERFORMANCE STATUS
    // =====================================================

    String performanceStatus;

    if (achievementPercentage.compareTo(
            BigDecimal.valueOf(100)) >= 0) {

        performanceStatus =
                "Target Achieved";

    } else if (achievementPercentage.compareTo(
            BigDecimal.valueOf(75)) >= 0) {

        performanceStatus =
                "Excellent Progress";

    } else if (achievementPercentage.compareTo(
            BigDecimal.valueOf(50)) >= 0) {

        performanceStatus =
                "Needs Attention";

    } else {

        performanceStatus =
                "Low Performance";
    }


    // =====================================================
    // CLIENT TARGET
    // =====================================================

    Integer clientTarget =
            target != null
                    && target.getClientTarget() != null
                    ? target.getClientTarget()
                    : 0;


    // =====================================================
    // ACTUAL NEW CLIENTS
    // =====================================================

    long actualClients =
            clientRepository.countNewClientsForPeriod(
                    salesperson,
                    startDate,
                    endDate);


    // =====================================================
    // REMAINING CLIENTS
    // =====================================================

    long remainingClients =
            clientTarget - actualClients;

    if (remainingClients < 0) {

        remainingClients = 0;
    }


    // =====================================================
    // CLIENT ACHIEVEMENT %
    // =====================================================

    BigDecimal clientAchievementPercentage =
            BigDecimal.ZERO;

    if (clientTarget > 0) {

        clientAchievementPercentage =
                BigDecimal.valueOf(actualClients)
                        .multiply(
                                BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(clientTarget),
                                2,
                                java.math.RoundingMode.HALF_UP);
    }


    // =====================================================
    // CLIENT PROGRESS %
    // =====================================================

    BigDecimal clientProgressPercentage =
            clientAchievementPercentage;

    if (clientProgressPercentage.compareTo(
            BigDecimal.valueOf(100)) > 0) {

        clientProgressPercentage =
                BigDecimal.valueOf(100);
    }


    // =====================================================
    // CLIENT PERFORMANCE STATUS
    // =====================================================

    String clientPerformanceStatus;

    if (clientTarget == 0) {

        clientPerformanceStatus =
                "No Client Target Assigned";

    } else if (clientAchievementPercentage.compareTo(
            BigDecimal.valueOf(100)) >= 0) {

        clientPerformanceStatus =
                "Client Target Achieved";

    } else if (clientAchievementPercentage.compareTo(
            BigDecimal.valueOf(75)) >= 0) {

        clientPerformanceStatus =
                "Excellent Progress";

    } else if (clientAchievementPercentage.compareTo(
            BigDecimal.valueOf(50)) >= 0) {

        clientPerformanceStatus =
                "Needs Attention";

    } else {

        clientPerformanceStatus =
                "Low Client Performance";
    }


    // =====================================================
    // MODEL - SALES
    // =====================================================

    model.addAttribute(
            "salesperson",
            salesperson);

    model.addAttribute(
            "selectedMonth",
            selectedMonthString);

    model.addAttribute(
            "targetAmount",
            targetAmount);

    model.addAttribute(
            "achievedAmount",
            achievedAmount);

    model.addAttribute(
            "remainingAmount",
            remainingAmount);

    model.addAttribute(
            "achievementPercentage",
            achievementPercentage);

    model.addAttribute(
            "progressPercentage",
            progressPercentage);

    model.addAttribute(
            "performanceStatus",
            performanceStatus);

    model.addAttribute(
            "monthlySales",
            monthlySales);


    // =====================================================
    // MODEL - CLIENTS
    // =====================================================

    model.addAttribute(
            "clientTarget",
            clientTarget);

    model.addAttribute(
            "actualClients",
            actualClients);

    model.addAttribute(
            "remainingClients",
            remainingClients);

    model.addAttribute(
            "clientAchievementPercentage",
            clientAchievementPercentage);

    model.addAttribute(
            "clientProgressPercentage",
            clientProgressPercentage);

    model.addAttribute(
            "clientPerformanceStatus",
            clientPerformanceStatus);


    return "my-report";
}

    // =========================================================
    // ADD MY SALE
    // =========================================================

    @GetMapping("/sales/add")
    public String addMySale(
            Authentication authentication,
            Model model) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        model.addAttribute(
                "salesperson",
                salesperson);

        model.addAttribute(
                "sale",
                new Sale());

        return "sales-add";
    }

    // =========================================================
    // SAVE MY SALE
    // =========================================================

    @PostMapping("/sales/save")
    public String saveMySale(
            Authentication authentication,

            @RequestParam
            String invoiceNumber,

            @RequestParam
            String saleDate,

            @RequestParam
            String customerName,

            @RequestParam("productName")
            List<String> productNames,

            @RequestParam("quantity")
            List<BigDecimal> quantities,

            @RequestParam("unit")
            List<String> units,

            @RequestParam("price")
            List<BigDecimal> prices) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        Sale sale =
                new Sale();

        // Salesperson authentication se set hoga.
        // Form se salesperson nahi liya jayega.

        sale.setSalesperson(
                salesperson);

        sale.setInvoiceNumber(
                invoiceNumber);

        sale.setSaleDate(
                LocalDate.parse(saleDate));

        sale.setCustomerName(
                customerName);

        // =====================================================
        // MULTIPLE ITEMS
        // =====================================================

        for (int i = 0;
             i < productNames.size();
             i++) {

            if (productNames.get(i) == null ||
                    productNames.get(i).isBlank()) {

                continue;
            }

            SaleItem item =
                    new SaleItem();

            item.setProductName(
                    productNames.get(i));

            item.setQuantity(
                    quantities.get(i));

            item.setUnit(
                    units.get(i));

            item.setPrice(
                    prices.get(i));

            item.calculateAmount();

            sale.addItem(item);
        }

        // =====================================================
        // RECALCULATE TOTAL
        // =====================================================

        sale.calculateTotal();

        saleRepository.save(sale);

        return "redirect:/sales/my-sales";
    }

    // =========================================================
    // EDIT MY SALE
    // =========================================================

    @GetMapping("/sales/edit/{id}")
    public String editMySale(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        Sale sale =
                saleRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Sale not found"));

        // =====================================================
        // SECURITY CHECK
        // =====================================================

        if (sale.getSalesperson() == null ||
                !sale.getSalesperson()
                        .getId()
                        .equals(salesperson.getId())) {

            throw new RuntimeException(
                    "You are not allowed to edit this sale");
        }

        model.addAttribute(
                "salesperson",
                salesperson);

        model.addAttribute(
                "sale",
                sale);

        return "sales-edit";
    }

    // =========================================================
    // UPDATE MY SALE
    // =========================================================

    @PostMapping("/sales/update/{id}")
    public String updateMySale(
            @PathVariable Long id,
            Authentication authentication,

            @RequestParam
            String saleDate,

            @RequestParam
            String customerName,

            @RequestParam("productName")
            List<String> productNames,

            @RequestParam("quantity")
            List<BigDecimal> quantities,

            @RequestParam("unit")
            List<String> units,

            @RequestParam("price")
            List<BigDecimal> prices) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        Sale sale =
                saleRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Sale not found"));

        // =====================================================
        // SECURITY CHECK
        // =====================================================

        if (sale.getSalesperson() == null ||
                !sale.getSalesperson()
                        .getId()
                        .equals(salesperson.getId())) {

            throw new RuntimeException(
                    "You are not allowed to edit this sale");
        }

        sale.setSaleDate(
                LocalDate.parse(saleDate));

        sale.setCustomerName(
                customerName);

        /*
         * Invoice number aur salesperson ko
         * salesperson change nahi kar sakta.
         */

        // =====================================================
        // OLD ITEMS REMOVE
        // =====================================================

        sale.getItems().clear();

        // =====================================================
        // NEW ITEMS
        // =====================================================

        for (int i = 0;
             i < productNames.size();
             i++) {

            if (productNames.get(i) == null ||
                    productNames.get(i).isBlank()) {

                continue;
            }

            SaleItem item =
                    new SaleItem();

            item.setProductName(
                    productNames.get(i));

            item.setQuantity(
                    quantities.get(i));

            item.setUnit(
                    units.get(i));

            item.setPrice(
                    prices.get(i));

            item.calculateAmount();

            sale.addItem(item);
        }

        // =====================================================
        // RECALCULATE TOTAL
        // =====================================================

        sale.calculateTotal();

        saleRepository.save(sale);

        // Month parameter removed because
        // My Sales now uses customer/date filters.

        return "redirect:/sales/my-sales";
    }

    // =========================================================
    // HELPER - LOGGED IN SALESPERSON
    // =========================================================

    private User getLoggedInSalesperson(
            Authentication authentication) {

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Salesperson not found"));
    }

    // =========================================================
    // HELPER - SELECTED MONTH
    // =========================================================

    private YearMonth getSelectedMonth(
            String month) {

        if (month == null ||
                month.isBlank()) {

            return YearMonth.now();
        }

        return YearMonth.parse(month);
    }

    // =========================================================
    // HELPER - PERCENTAGE
    // =========================================================

    private BigDecimal calculatePercentage(
            BigDecimal achieved,
            BigDecimal target) {

        if (target == null ||
                target.compareTo(
                        BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        if (achieved == null) {

            achieved =
                    BigDecimal.ZERO;
        }

        return achieved
                .multiply(
                        BigDecimal.valueOf(100))
                .divide(
                        target,
                        2,
                        RoundingMode.HALF_UP);
    }

    // =========================================================
    // VIEW MY SALE
    // =========================================================
// =========================================================
// VIEW MY SALE
// =========================================================

@GetMapping("/my-sales/view/{id}")
public String viewMySale(
        @PathVariable("id") Long id,
        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    Sale sale =
            saleRepository.findById(id)
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Sale not found: " + id));

    // Security check:
    // Salesperson sirf apni sale dekh sakta hai
    if (sale.getSalesperson() == null ||
            !sale.getSalesperson()
                    .getId()
                    .equals(salesperson.getId())) {

        throw new RuntimeException(
                "You are not allowed to view this sale");
    }

    // Lazy items load
    sale.getItems().size();

    model.addAttribute(
            "salesperson",
            salesperson);

    model.addAttribute(
            "sale",
            sale);

    return "sales-view";
}

    // =========================================================
    // IMPORT SALES EXCEL PAGE
    // =========================================================

    @GetMapping("/sales/import")
    public String showImportPage(
            Authentication authentication,
            Model model) {

        User salesperson =
                getLoggedInSalesperson(authentication);

        model.addAttribute(
                "salesperson",
                salesperson);

        return "sales-import";
    }

    // =========================================================
    // IMPORT SALES EXCEL
    // =========================================================

    // =========================================================
// PREVIEW SALES EXCEL
// =========================================================

@PostMapping("/sales/import-preview")
public String previewSalesExcel(

        @RequestParam("file")
        MultipartFile file,

        Authentication authentication,

        Model model,

        HttpSession session,

        RedirectAttributes redirectAttributes) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    try {

        // =================================================
        // FILE VALIDATION
        // =================================================

        if (file == null ||
                file.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select an Excel file.");

            return "redirect:/sales/sales/import";
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                (!fileName.toLowerCase()
                        .endsWith(".xlsx")
                &&
                !fileName.toLowerCase()
                        .endsWith(".xls"))) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only Excel files (.xlsx or .xls) are allowed.");

            return "redirect:/sales/sales/import";
        }

        // =================================================
        // CREATE PREVIEW
        // =================================================

        SalesImportPreview preview =
                salesExcelImportService.previewSales(
                        file,
                        salesperson);

        // =================================================
        // STORE PREVIEW IN SESSION
        // =================================================

        session.setAttribute(
                "SALES_IMPORT_PREVIEW",
                preview);

        // =================================================
        // MODEL
        // =================================================

        model.addAttribute(
                "salesperson",
                salesperson);

        model.addAttribute(
                "preview",
                preview);

        return "sales-import-preview";

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Excel preview failed: "
                        + e.getMessage());

        return "redirect:/sales/sales/import";
    }
}
// =========================================================
// CONFIRM SALES EXCEL IMPORT
// =========================================================

@PostMapping("/sales/import-confirm")
public String confirmSalesImport(

        Authentication authentication,

        HttpSession session,

        RedirectAttributes redirectAttributes) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    try {

        Object previewObject =
                session.getAttribute(
                        "SALES_IMPORT_PREVIEW");

        if (!(previewObject
                instanceof SalesImportPreview)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Preview session expired. Please upload Excel again.");

            return "redirect:/sales/sales/import";
        }

        SalesImportPreview preview =
                (SalesImportPreview)
                        previewObject;

        // =================================================
        // FINAL SAVE
        // =================================================

        int importedCount =
                salesExcelImportService.savePreview(
                        preview,
                        salesperson);

        // =================================================
        // REMOVE SESSION PREVIEW
        // =================================================

        session.removeAttribute(
                "SALES_IMPORT_PREVIEW");

        redirectAttributes.addFlashAttribute(
                "successMessage",
                importedCount
                        + " sales imported successfully.");

        return "redirect:/sales/my-sales";

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Excel import failed: "
                        + e.getMessage());

        return "redirect:/sales/sales/import";
    }
}// =========================
// MY CLIENTS
// =========================

@GetMapping("/my-clients")
public String myClients(
        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    List<Client> clients =
            clientRepository
                    .findBySalespersonOrderByCreatedDateDesc(
                            salesperson);

    model.addAttribute("salesperson", salesperson);
    model.addAttribute("clients", clients);

    return "my-clients";
}


// =========================
// ADD CLIENT PAGE
// =========================
@GetMapping("/client/add")
public String addClient(
        Authentication authentication,
        Model model) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    model.addAttribute("salesperson", salesperson);
    model.addAttribute("client", new Client());

    return "client-add";
}


// =========================
// SAVE CLIENT
// =========================

@PostMapping("/client/save")
public String saveClient(
        @ModelAttribute Client client,
        Authentication authentication) {

    User salesperson =
            getLoggedInSalesperson(authentication);

    client.setSalesperson(salesperson);
    client.setCreatedDate(LocalDate.now());
    client.setStatus("ACTIVE");

    clientRepository.save(client);

    return "redirect:/sales/my-clients";
}
    
}