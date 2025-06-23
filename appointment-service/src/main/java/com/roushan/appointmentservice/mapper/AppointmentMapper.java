package com.roushan.appointmentservice.mapper;

import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.model.Appointment;
import com.roushan.appointmentservice.model.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AppointmentMapper {

    public static AppointmentResponseDTO toDTO(Appointment appointment) {
        return AppointmentResponseDTO.builder()
                .id(String.valueOf(appointment.getId()))
                .patientId(String.valueOf(appointment.getPatientId()))
                .doctorId(String.valueOf(appointment.getDoctorId()))
                .appointmentDateTime(String.valueOf(appointment.getAppointmentDateTime()))
                .status(appointment.getStatus().name())
                .build();
    }

    public static Appointment toModel(AppointmentRequestDTO requestDTO) {
        Appointment appointment = new Appointment();
        // PatientId is now UUID in DTO, can directly set if provided
        // Logic for setting patientId from token or DTO is in service, so just transfer
        appointment.setPatientId(UUID.fromString(requestDTO.getPatientId())); // Now UUID
        appointment.setDoctorId(UUID.fromString(requestDTO.getDoctorId())); // Now UUID
        appointment.setAppointmentDateTime(LocalDateTime.parse(requestDTO.getAppointmentDateTime())); // Parse from String
        appointment.setStatus(
                requestDTO.getStatus() != null && !requestDTO.getStatus().isEmpty()
                        ? AppointmentStatus.valueOf(requestDTO.getStatus().toUpperCase())
                        : AppointmentStatus.SCHEDULED // Default status
        );
        return appointment;
    }
}