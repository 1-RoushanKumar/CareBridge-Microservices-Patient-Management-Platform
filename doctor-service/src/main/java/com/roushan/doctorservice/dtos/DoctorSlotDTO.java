// src/main/java/com/roushan/doctorservice/dtos/DoctorSlotDTO.java
package com.roushan.doctorservice.dtos;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DoctorSlotDTO {
    private UUID id; // For response, if a slot ID exists
    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;
    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Slot start time cannot be in the past")
    private LocalDateTime startTime;
    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "Slot end time cannot be in the past")
    private LocalDateTime endTime;
    private boolean isBooked;
    private UUID appointmentId; // Only if booked
}