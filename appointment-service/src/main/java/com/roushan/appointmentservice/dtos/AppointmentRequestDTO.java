package com.roushan.appointmentservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AppointmentRequestDTO {

    // Patient ID is optional for "book own appointment" but required for "book for others"
    // We'll handle this in the service layer based on user roles
    private String patientId; // UUID as String

    @NotBlank(message = "Doctor ID is required")
    private String doctorId; // UUID as String

    @NotBlank(message = "Appointment date and time is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}", message = "Date and time must be in yyyy-MM-ddTHH:mm format")
    private String appointmentDateTime; // ISO-8601 format for easy parsing

    // Optional: for specific use cases like rescheduling or cancelling an existing one
    private String status; // For setting initial status or updating

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(String appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
