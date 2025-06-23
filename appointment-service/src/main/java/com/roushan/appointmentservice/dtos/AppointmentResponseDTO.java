package com.roushan.appointmentservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDTO {
    private String id;
    private String patientId;
    private String doctorId;
    private String appointmentDateTime;
    private String status;
}
