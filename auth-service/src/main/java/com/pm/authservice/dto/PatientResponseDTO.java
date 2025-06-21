package com.pm.authservice.dto;

import java.time.LocalDate; // Import if your PatientResponseDTO uses LocalDate
import java.util.UUID;

// This DTO must match the PatientResponseDTO from your patient-service
// as returned by the /patients/by-email endpoint.
public class PatientResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String address;
    private LocalDate dateOfBirth; // Use LocalDate if in patient-service DTO
    private String contact;
    private String gender;
    private String emergencyContact;
    private LocalDate registeredDate; // Use LocalDate if in patient-service DTO

    public PatientResponseDTO(UUID id, String name, String email, String address, LocalDate dateOfBirth, String contact, String gender, String emergencyContact, LocalDate registeredDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.contact = contact;
        this.gender = gender;
        this.emergencyContact = emergencyContact;
        this.registeredDate = registeredDate;
    }

    public PatientResponseDTO() {
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }
}