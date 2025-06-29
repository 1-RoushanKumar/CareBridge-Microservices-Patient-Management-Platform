// src/main/java/com/roushan/doctorservice/dtos/DoctorRequestDTO.java
package com.roushan.doctorservice.dtos;

import com.roushan.doctorservice.model.DoctorStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorRequestDTO {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Specialization is required")
    private String specialization;
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    @Min(value = 0, message = "Experience years cannot be negative")
    private int experienceYears;
    private String clinicAddress;
    @NotNull(message = "Status is required")
    private DoctorStatus status;
}