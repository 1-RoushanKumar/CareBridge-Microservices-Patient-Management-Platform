package com.roushan.appointmentservice.mapper;

import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.model.Appointment;
import com.roushan.appointmentservice.model.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AppointmentMapper {

    public static AppointmentResponseDTO toDTO(Appointment appointment) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(appointment.getId().toString());
        dto.setPatientId(appointment.getPatientId().toString());
        dto.setDoctorId(appointment.getDoctorId().toString());
        dto.setAppointmentDateTime(appointment.getAppointmentDateTime().toString());
        dto.setStatus(appointment.getStatus().name());
        return dto;
    }

    public static Appointment toModel(AppointmentRequestDTO requestDTO) {
        Appointment appointment = new Appointment();
        // Patient ID will be set by the service layer based on context (logged-in user or admin providing it)
        if (requestDTO.getPatientId() != null && !requestDTO.getPatientId().isEmpty()) {
            appointment.setPatientId(UUID.fromString(requestDTO.getPatientId()));
        }
        appointment.setDoctorId(UUID.fromString(requestDTO.getDoctorId()));
        appointment.setAppointmentDateTime(LocalDateTime.parse(requestDTO.getAppointmentDateTime()));
        // Set initial status, can be overridden if provided in DTO
        appointment.setStatus(
                requestDTO.getStatus() != null && !requestDTO.getStatus().isEmpty()
                        ? AppointmentStatus.valueOf(requestDTO.getStatus().toUpperCase())
                        : AppointmentStatus.SCHEDULED // Default status
        );
        return appointment;
    }
}
