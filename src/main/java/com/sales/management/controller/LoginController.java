package com.sales.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String home() {
        return "home";
    }


    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }


    @GetMapping("/sales/login")
    public String salesLogin() {
        return "sales-login";
    }

}