package com.sales.management.controller;

import com.sales.management.model.Sale;
import com.sales.management.model.SaleItem;
import com.sales.management.model.SalesReport;
import com.sales.management.model.Target;
import com.sales.management.model.User;
import com.sales.management.repository.ClientRepository;
import com.sales.management.repository.SaleItemRepository;
import com.sales.management.repository.SaleRepository;
import com.sales.management.repository.TargetRepository;
import com.sales.management.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TargetRepository targetRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ClientRepository clientRepository;

    public AdminController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TargetRepository targetRepository,
            SaleRepository saleRepository,
            ClientRepository clientRepository,

            SaleItemRepository saleItemRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.targetRepository = targetRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.clientRepository = clientRepository;
    }

    // =========================================================
    // ADMIN DASHBOARD
    // =========================================================

    // =========================================================
// ADMIN DASHBOARD
// =========================================================

@GetMapping("/dashboard")
public String dashboard(
        @RequestParam(
                name = "month",
                required = false
        ) String month,
        Model model) {

    YearMonth selectedMonth =
            getSelectedMonth(month);

    String selectedMonthString =
            selectedMonth.toString();

    LocalDate startDate =
            selectedMonth.atDay(1);

    LocalDate endDate =
            selectedMonth.atEndOfMonth();


    // =====================================================
    // SALESPERSON COUNTS
    // =====================================================

    long totalSalespersons =
            userRepository.countByRole("SALESPERSON");

    long activeSalespersons =
            userRepository.countByRoleAndActive(
                    "SALESPERSON",
                    true);

    long inactiveSalespersons =
            userRepository.countByRoleAndActive(
                    "SALESPERSON",
                    false);


    // =====================================================
    // SALESPERSONS
    // =====================================================

    List<User> salespersons =
            userRepository.findByRole("SALESPERSON");


    // =====================================================
    // TOTAL SALES TARGET
    // =====================================================

    BigDecimal totalTarget =
            salespersons
                    .stream()
                    .map(salesperson ->
                            targetRepository
                                    .findBySalespersonAndTargetMonth(
                                            salesperson,
                                            selectedMonthString)
                                    .map(Target::getTargetAmount)
                                    .orElse(BigDecimal.ZERO))
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add);


    // =====================================================
    // TOTAL SALES ACHIEVED
    // =====================================================

    BigDecimal totalAchieved =
            salespersons
                    .stream()
                    .flatMap(salesperson ->
                            saleRepository
                                    .findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
                                            salesperson,
                                            startDate,
                                            endDate)
                                    .stream())
                    .map(Sale::getTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add);


    // =====================================================
    // SALES ACHIEVEMENT %
    // =====================================================

    double achievementPercentage = 0;

    if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {

        achievementPercentage =
                totalAchieved
                        .divide(
                                totalTarget,
                                4,
                                java.math.RoundingMode.HALF_UP)
                        .doubleValue()
                        * 100;
    }


    // =====================================================
    // TOTAL CLIENT TARGET
    // =====================================================

    long totalClientTarget = 0;

    for (User salesperson : salespersons) {

        Integer clientTarget =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString)
                        .map(Target::getClientTarget)
                        .orElse(0);

        totalClientTarget += clientTarget;
    }


    // =====================================================
    // TOTAL NEW CLIENTS
    // =====================================================

    long totalNewClients = 0;

    for (User salesperson : salespersons) {

        totalNewClients +=
                clientRepository.countNewClientsForPeriod(
                        salesperson,
                        startDate,
                        endDate);
    }


    // =====================================================
    // CLIENT ACHIEVEMENT %
    // =====================================================

    double clientAchievementPercentage = 0;

    if (totalClientTarget > 0) {

        clientAchievementPercentage =
                ((double) totalNewClients
                        / totalClientTarget)
                        * 100;
    }


    // =====================================================
    // SALESPERSON PERFORMANCE
    // =====================================================

    List<SalesReport> reports =
            new java.util.ArrayList<>();


    for (User salesperson : salespersons) {


        // -------------------------------------------------
        // SALES TARGET
        // -------------------------------------------------

        BigDecimal targetAmount =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString)
                        .map(Target::getTargetAmount)
                        .orElse(BigDecimal.ZERO);


        // -------------------------------------------------
        // ACHIEVED SALES
        // -------------------------------------------------

        BigDecimal achievedAmount =
                saleRepository
                        .findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
                                salesperson,
                                startDate,
                                endDate)
                        .stream()
                        .map(Sale::getTotalAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add);


        // -------------------------------------------------
        // TOTAL PRODUCT QUANTITY
        // -------------------------------------------------

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


        // -------------------------------------------------
        // CLIENT TARGET
        // -------------------------------------------------

        Integer clientTarget =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString)
                        .map(Target::getClientTarget)
                        .orElse(0);


        // -------------------------------------------------
        // ACTUAL NEW CLIENTS
        // -------------------------------------------------

        long actualClients =
                clientRepository.countNewClientsForPeriod(
                        salesperson,
                        startDate,
                        endDate);


        // -------------------------------------------------
        // CREATE REPORT
        // -------------------------------------------------

        reports.add(
                new SalesReport(
                        salesperson,
                        targetAmount,
                        achievedAmount,
                        totalProductQuantity,
                        clientTarget,
                        actualClients));
    }


    // =====================================================
    // MODEL ATTRIBUTES
    // =====================================================

    model.addAttribute(
            "totalSalespersons",
            totalSalespersons);

    model.addAttribute(
            "activeSalespersons",
            activeSalespersons);

    model.addAttribute(
            "inactiveSalespersons",
            inactiveSalespersons);

    model.addAttribute(
            "totalTarget",
            totalTarget);

    model.addAttribute(
            "totalAchieved",
            totalAchieved);

    model.addAttribute(
            "achievementPercentage",
            achievementPercentage);

    model.addAttribute(
            "totalClientTarget",
            totalClientTarget);

    model.addAttribute(
            "totalNewClients",
            totalNewClients);

    model.addAttribute(
            "clientAchievementPercentage",
            clientAchievementPercentage);

    model.addAttribute(
            "selectedMonth",
            selectedMonthString);

    model.addAttribute(
            "reports",
            reports);


    return "admin-dashboard";
}

    // =========================================================
    // SALESPEOPLE LIST
    // =========================================================

    @GetMapping("/salespersons")
    public String salespersons(Model model) {

        model.addAttribute(
                "salespersons",
                userRepository.findByRole("SALESPERSON"));

        return "salespersons";
    }

    // =========================================================
    // ADD SALESPERSON
    // =========================================================

    @GetMapping("/salespersons/add")
    public String addSalespersonPage(Model model) {

        model.addAttribute(
                "user",
                new User());

        return "add-salesperson";
    }

    // =========================================================
    // SAVE SALESPERSON
    // =========================================================

    @PostMapping("/salespersons/save")
    public String saveSalesperson(
            @ModelAttribute User user) {

        user.setRole("SALESPERSON");

        user.setActive(true);

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()));

        userRepository.save(user);

        return "redirect:/admin/salespersons";
    }

    // =========================================================
    // DELETE SALESPERSON
    // =========================================================

    @GetMapping("/salespersons/delete/{id}")
    public String deleteSalesperson(
            @PathVariable Long id) {

        userRepository.deleteById(id);

        return "redirect:/admin/salespersons";
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    @GetMapping("/salespersons/toggle/{id}")
    public String toggleSalesperson(
            @PathVariable Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElse(null);

        if (user != null) {

            user.setActive(
                    !user.isActive());

            userRepository.save(user);
        }

        return "redirect:/admin/salespersons";
    }

    // =========================================================
    // TARGET LIST
    // =========================================================

    @GetMapping("/targets")
    public String targets(
            @RequestParam(
                    name = "month",
                    required = false
            ) String month,
            Model model) {

        YearMonth selectedMonth =
                getSelectedMonth(month);

        String selectedMonthString =
                selectedMonth.toString();

        List<Target> targets =
                targetRepository
                        .findByTargetMonth(
                                selectedMonthString);

        model.addAttribute(
                "targets",
                targets);

        model.addAttribute(
                "selectedMonth",
                selectedMonthString);

        return "targets";
    }

    // =========================================================
    // ADD TARGET
    // =========================================================

    @GetMapping("/targets/add")
    public String addTarget(Model model) {

        model.addAttribute(
                "salespersons",
                userRepository.findByRole("SALESPERSON"));

        model.addAttribute(
                "target",
                new Target());

        return "add-target";
    }

    // =========================================================
    // SAVE / UPDATE TARGET
    // =========================================================

    @PostMapping("/targets/save")
public String saveTarget(
        @RequestParam Long salespersonId,
        @RequestParam String targetMonth,
        @RequestParam BigDecimal targetAmount,
        @RequestParam Integer clientTarget,
        @RequestParam(required = false) Long targetId) {

    User salesperson =
            userRepository
                    .findById(salespersonId)
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Salesperson not found"));

    Target target;

    if (targetId != null) {

        target =
                targetRepository
                        .findById(targetId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Target not found"));

    } else {

        target =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                targetMonth)
                        .orElse(new Target());
    }

    target.setSalesperson(salesperson);

    target.setTargetMonth(targetMonth);

    target.setTargetAmount(targetAmount);

    // Client Target
    target.setClientTarget(clientTarget);

    targetRepository.save(target);

    return "redirect:/admin/targets";
}

    // =========================================================
    // DELETE TARGET
    // =========================================================

    @GetMapping("/targets/delete/{id}")
    public String deleteTarget(
            @PathVariable Long id) {

        targetRepository.deleteById(id);

        return "redirect:/admin/targets";
    }

    // =========================================================
    // SALES LIST
    // =========================================================
@GetMapping("/sales")
public String sales(
        @RequestParam(
                name = "month",
                required = false
        ) String month,
        Model model) {

    YearMonth selectedMonth =
            getSelectedMonth(month);

    LocalDate startDate =
            selectedMonth.atDay(1);

    LocalDate endDate =
            selectedMonth.atEndOfMonth();

    List<Sale> sales =
            saleRepository
                    .findBySaleDateBetweenOrderBySaleDateDesc(
                            startDate,
                            endDate);

    model.addAttribute(
            "sales",
            sales);

    model.addAttribute(
            "selectedMonth",
            selectedMonth.toString());

    // SALESPERSON DROPDOWN
    model.addAttribute(
            "salespersons",
            userRepository.findAll());

    return "sales";
}

    // =========================================================
    // ADD SALE
    // =========================================================

    @GetMapping("/sales/add")
    public String addSale(Model model) {

        model.addAttribute(
                "salespersons",
                userRepository.findByRole("SALESPERSON"));

        model.addAttribute(
                "sale",
                new Sale());

        return "add-sale";
    }

    // =========================================================
    // SAVE SALE
    // =========================================================

    @PostMapping("/sales/save")
    public String saveSale(

            @RequestParam Long salespersonId,

            @RequestParam String invoiceNumber,

            @RequestParam String saleDate,

            @RequestParam String customerName,

            @RequestParam("productName")
            List<String> productNames,

            @RequestParam("quantity")
            List<BigDecimal> quantities,

            @RequestParam("unit")
            List<String> units,

            @RequestParam("price")
            List<BigDecimal> prices) {

        User salesperson =
                userRepository
                        .findById(salespersonId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Salesperson not found"));

        Sale sale = new Sale();

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
        // CALCULATE TOTAL
        // =====================================================

        sale.calculateTotal();

        saleRepository.save(sale);

        return "redirect:/admin/sales";
    }

    // =========================================================
    // DELETE SALE
    // =========================================================

    @GetMapping("/sales/delete/{id}")
    public String deleteSale(
            @PathVariable Long id) {

        saleRepository.deleteById(id);

        return "redirect:/admin/sales";
    }

    // =========================================================
    // REPORTS
    // =========================================================

    @GetMapping("/reports")
public String reports(
        @RequestParam(
                name = "month",
                required = false
        ) String month,
        Model model) {

    // =====================================================
    // SELECTED MONTH
    // =====================================================

    YearMonth selectedMonth = getSelectedMonth(month);

    String selectedMonthString = selectedMonth.toString();

    LocalDate startDate = selectedMonth.atDay(1);
    LocalDate endDate = selectedMonth.atEndOfMonth();


    // =====================================================
    // GET ALL SALESPERSONS
    // =====================================================

    List<User> salespersons =
            userRepository.findByRole("SALESPERSON");


    // =====================================================
    // CREATE REPORT LIST
    // =====================================================

    List<SalesReport> reports =
            new java.util.ArrayList<>();


    // =====================================================
    // CREATE REPORT FOR EACH SALESPERSON
    // =====================================================

    for (User salesperson : salespersons) {

        // -------------------------------------------------
        // TARGET AMOUNT
        // -------------------------------------------------

        BigDecimal targetAmount =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString
                        )
                        .map(Target::getTargetAmount)
                        .orElse(BigDecimal.ZERO);


        // -------------------------------------------------
        // ACHIEVED SALES AMOUNT
        // -------------------------------------------------

        BigDecimal achievedAmount =
                saleRepository
                        .findBySalespersonAndSaleDateBetweenOrderBySaleDateDesc(
                                salesperson,
                                startDate,
                                endDate
                        )
                        .stream()
                        .map(Sale::getTotalAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        // -------------------------------------------------
        // TOTAL PRODUCT QUANTITY
        // -------------------------------------------------

        BigDecimal totalProductQuantity =
                saleRepository
                        .getTotalProductQuantityForPeriod(
                                salesperson,
                                startDate,
                                endDate
                        );

        if (totalProductQuantity == null) {
            totalProductQuantity = BigDecimal.ZERO;
        }


        // -------------------------------------------------
        // CLIENT TARGET
        // -------------------------------------------------

        Integer clientTarget =
                targetRepository
                        .findBySalespersonAndTargetMonth(
                                salesperson,
                                selectedMonthString
                        )
                        .map(Target::getClientTarget)
                        .orElse(0);


        // -------------------------------------------------
        // ACTUAL CLIENTS
        // -------------------------------------------------

        long actualClients =
                clientRepository
                        .countBySalespersonAndCreatedDateBetween(
                                salesperson,
                                startDate,
                                endDate
                        );


        // -------------------------------------------------
        // CREATE SALES REPORT
        // -------------------------------------------------

        reports.add(
                new SalesReport(
                        salesperson,
                        targetAmount,
                        achievedAmount,
                        totalProductQuantity,
                        clientTarget,
                        actualClients
                )
        );
    }


    // =====================================================
    // SORT REPORTS BY ACHIEVED AMOUNT
    // LOW → HIGH
    // =====================================================

    reports.sort(
            java.util.Comparator.comparing(
                    SalesReport::getAchieved
            )
    );


    // =====================================================
    // SEND DATA TO THYMELEAF
    // =====================================================

    model.addAttribute(
            "reports",
            reports
    );

    model.addAttribute(
            "selectedMonth",
            selectedMonthString
    );


    // =====================================================
    // RETURN REPORTS PAGE
    // =====================================================

    return "reports";
}

    // =========================================================
    // EDIT TARGET
    // =========================================================

    @GetMapping("/targets/edit/{id}")
    public String editTarget(
            @PathVariable Long id,
            Model model) {

        Target target =
                targetRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Target not found"));

        model.addAttribute(
                "target",
                target);

        model.addAttribute(
                "salespersons",
                userRepository.findByRole(
                        "SALESPERSON"));

        return "edit-target";
    }

    // =========================================================
    // EDIT SALE
    // =========================================================

    @GetMapping("/sales/edit/{id}")
    public String editSale(
            @PathVariable Long id,
            Model model) {

        Sale sale =
                saleRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Sale not found"));

        model.addAttribute(
                "sale",
                sale);

        model.addAttribute(
                "salespersons",
                userRepository.findByRole(
                        "SALESPERSON"));

        return "edit-sale";
    }

    // =========================================================
    // UPDATE SALE
    // =========================================================

    @PostMapping("/sales/update")
    public String updateSale(

            @RequestParam Long id,

            @RequestParam Long salespersonId,

            @RequestParam String invoiceNumber,

            @RequestParam String saleDate,

            @RequestParam String customerName,

            @RequestParam("productName")
            List<String> productNames,

            @RequestParam("quantity")
            List<BigDecimal> quantities,

            @RequestParam("unit")
            List<String> units,

            @RequestParam("price")
            List<BigDecimal> prices) {

        Sale sale =
                saleRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Sale not found"));

        User salesperson =
                userRepository
                        .findById(salespersonId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Salesperson not found"));

        sale.setSalesperson(
                salesperson);

        sale.setInvoiceNumber(
                invoiceNumber);

        sale.setSaleDate(
                LocalDate.parse(saleDate));

        sale.setCustomerName(
                customerName);

        // =====================================================
        // REMOVE OLD ITEMS
        // =====================================================

        sale.getItems().clear();

        // =====================================================
        // ADD UPDATED ITEMS
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
        // RECALCULATE INVOICE TOTAL
        // =====================================================

        sale.calculateTotal();

        saleRepository.save(sale);

        return "redirect:/admin/sales";
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
// ADMIN - VIEW SALE
// =========================================================

// =========================================================
// ADMIN - VIEW SALE
// =========================================================

@GetMapping("/sales/view/{id}")
public String viewSale(
        @PathVariable("id") Long id,
        Model model) {

    System.out.println("=================================");
    System.out.println("ADMIN VIEW SALE METHOD CALLED");
    System.out.println("SALE ID = " + id);
    System.out.println("=================================");

    Sale sale = saleRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Sale not found: " + id));

    // Load sale items
    sale.getItems().size();

    model.addAttribute("sale", sale);

    return "sale-view";
}

}