package com.sales.management.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /*
     * Existing application fields
     */
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "name")
    private String name;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "city")
    private String city;

    @Column(name = "active")
    private boolean active = true;

    /*
     * Sales relationship
     */
    @OneToMany(mappedBy = "salesperson", cascade = CascadeType.ALL)
    private List<Sale> sales = new ArrayList<>();

    /*
     * Target relationship
     */
    @OneToMany(mappedBy = "salesperson", cascade = CascadeType.ALL)
    private List<Target> targets = new ArrayList<>();

    public User() {
    }

    // =========================
    // ID
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // =========================
    // Username
    // =========================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // =========================
    // Password
    // =========================

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // =========================
    // Full Name
    // =========================

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // =========================
    // Email
    // =========================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // =========================
    // Role
    // =========================

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // =========================
    // Enabled
    // =========================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // =========================
    // Employee ID
    // =========================

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    // =========================
    // Name
    // =========================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // =========================
    // Mobile
    // =========================

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    // =========================
    // City
    // =========================

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    // =========================
    // Active
    // =========================

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // =========================
    // Sales
    // =========================

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    // =========================
    // Targets
    // =========================

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }
}