package com.sales.management.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // Salesperson
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesperson_id", nullable = false)
    private User salesperson;


    // =========================
    // Client Details
    // =========================

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "gst_no")
    private String gstNo;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;


    // =========================
    // Date
    // =========================

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;


    // =========================
    // Status
    // =========================

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";


    // =========================
    // Constructor
    // =========================

    public Client() {
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
    // Salesperson
    // =========================

    public User getSalesperson() {
        return salesperson;
    }

    public void setSalesperson(User salesperson) {
        this.salesperson = salesperson;
    }


    // =========================
    // Client Name
    // =========================

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }


    // =========================
    // Company Name
    // =========================

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    // =========================
    // GST Number
    // =========================

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
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
    // Email
    // =========================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // =========================
    // Address
    // =========================

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    // =========================
    // Created Date
    // =========================

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }


    // =========================
    // Status
    // =========================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}