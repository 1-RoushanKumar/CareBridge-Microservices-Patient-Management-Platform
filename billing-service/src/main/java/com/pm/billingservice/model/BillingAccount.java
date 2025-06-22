package com.pm.billingservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id; // Primary key for the billing account itself

    @Column(unique = true, nullable = false)
    private UUID patientId; // Link to the Patient in patient-service

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String status; // E.g., "Active", "Inactive", "Suspended"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // You might add a balance, lastPaymentDate, etc., later

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
