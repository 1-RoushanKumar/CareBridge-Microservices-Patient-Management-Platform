package com.pm.patientservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits")
    private String contact; // New field

    @NotBlank(message = "Gender is required")
    private String gender; // New field

    @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits")
    private String emergencyContact; // New field

    public @NotBlank(message = "Name is required") @Size(max = 100, message = "Name cannot exceed 100 characters") String getName() {
        return name;
    }

    public void setName(
            @NotBlank(message = "Name is required") @Size(max = 100, message = "Name cannot exceed 100 characters") String name) {
        this.name = name;
    }

    public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
        return email;
    }

    public void setEmail(
            @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Address is required") String getAddress() {
        return address;
    }

    public void setAddress(
            @NotBlank(message = "Address is required") String address) {
        this.address = address;
    }

    public @NotBlank(message = "Date of birth is required") String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(
            @NotBlank(message = "Date of birth is required") String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public @NotBlank(message = "Contact number is required") @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits") String getContact() {
        return contact;
    }

    public void setContact(@NotBlank(message = "Contact number is required") @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits") String contact) {
        this.contact = contact;
    }

    public @NotBlank(message = "Gender is required") String getGender() {
        return gender;
    }

    public void setGender(@NotBlank(message = "Gender is required") String gender) {
        this.gender = gender;
    }

    public @NotBlank(message = "Emergency contact number is required") @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits") String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(@NotBlank(message = "Emergency contact number is required") @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits") String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}