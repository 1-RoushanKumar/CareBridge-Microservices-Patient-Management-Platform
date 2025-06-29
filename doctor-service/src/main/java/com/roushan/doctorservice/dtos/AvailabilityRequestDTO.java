package com.roushan.doctorservice.dtos;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AvailabilityRequestDTO {
    private UUID doctorId; // Optional: if checking for a specific doctor
    private String specialization; // Optional: if checking for any doctor in a specialty
    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Availability date cannot be in the past")
    private LocalDate date;
}