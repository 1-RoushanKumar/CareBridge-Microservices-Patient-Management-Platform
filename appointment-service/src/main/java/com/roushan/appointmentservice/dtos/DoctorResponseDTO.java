package com.roushan.appointmentservice.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DoctorResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private int experienceYears;
    private String clinicAddress;
    private DoctorStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double consultationFee;
}