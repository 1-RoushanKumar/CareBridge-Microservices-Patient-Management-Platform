// src/main/java/com/roushan/appointmentservice/model/Appointment.java (Modified)
package com.roushan.appointmentservice.model;

import com.roushan.appointmentservice.model.enums.AppointmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID doctorId;

    @NotNull
    private LocalDateTime appointmentDateTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // NEW FIELD to link to the specific slot in DoctorService
    private UUID doctorSlotId;
}