package com.roushan.appointmentservice.model;

import com.roushan.appointmentservice.model.enums.AppointmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    // Assuming a Patient can be identified by their UUID
    private UUID patientId;

    @NotNull
    // You might have a separate Doctor entity later, for now, just an ID
    private UUID doctorId;

    @NotNull
    private LocalDateTime appointmentDateTime;

    @NotNull
    @Enumerated(EnumType.STRING) // Store enum as String in DB
    private AppointmentStatus status; // Enum for status: SCHEDULED, CANCELED, COMPLETED, etc.

    // You can add more fields like:
    // private String reasonForAppointment;
    // private String notes; // For doctor's notes

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
