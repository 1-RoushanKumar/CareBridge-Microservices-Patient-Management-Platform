package com.roushan.appointmentservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequestDTO {

    private String patientId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotBlank(message = "Appointment date and time is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}", message = "Date and time must be in yyyy-MM-ddTHH:mm format")
    private String appointmentDateTime;

    private String status;

    @NotBlank(message = "Doctor slot ID is required for booking") // NEW FIELD
    private String doctorSlotId;
}
