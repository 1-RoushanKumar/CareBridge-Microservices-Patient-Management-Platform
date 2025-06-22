package com.roushan.appointmentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_details") // Recommend a distinct table name
public class PatientDetails {

    @Id
    @Column(columnDefinition = "uuid", unique = true, nullable = false)
    private UUID id; // This will store the patientId from the event, acting as the PK

    @NotNull
    private String name;

    @NotNull
    private String email;

    @NotNull
    // Store patient status (e.g., "ACTIVE", "INACTIVE", "DELETED")
    private String status;

    @NotNull
    private LocalDateTime lastUpdated; // Timestamp of the last event processed for this patient

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}