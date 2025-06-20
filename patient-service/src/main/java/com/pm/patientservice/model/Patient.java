package com.pm.patientservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Entity
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
    private String contact; // New field

    @NotNull
    private String gender; // New field

    @NotNull
    @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits")
    private String emergencyContact; // New field

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotNull @Email String email) {
        this.email = email;
    }

    public @NotNull String getAddress() {
        return address;
    }

    public void setAddress(@NotNull String address) {
        this.address = address;
    }

    public @NotNull LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(@NotNull LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public @NotNull LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(@NotNull LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }

    public @NotNull @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits") String getContact() {
        return contact;
    }

    public void setContact(@NotNull @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits") String contact) {
        this.contact = contact;
    }

    public @NotNull String getGender() {
        return gender;
    }

    public void setGender(@NotNull String gender) {
        this.gender = gender;
    }

    public @NotNull @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits") String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(@NotNull @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits") String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}