package com.pm.patientservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    @Email
    @Column(unique = true)
    private String email;

    @NotNull
    private String address;

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private LocalDate registeredDate;

    @NotNull
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits")
    @Column(unique = true)
    private String contact;

    @NotNull
    private String gender;

    @NotNull
    @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits")
    private String emergencyContact;

    @Column(nullable = false)
    private String billingAccountStatus = "PENDING_ACCOUNT_CREATION";
}