// src/main/java/com/roushan/doctorservice/model/Doctor.java
package com.roushan.doctorservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    private String phoneNumber;

    @Column(unique = true)
    private String licenseNumber;

    @Min(value = 0, message = "Experience years cannot be negative")
    private int experienceYears;

    private String clinicAddress;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private DoctorStatus status; // Enum for ACTIVE, ON_LEAVE, INACTIVE

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}