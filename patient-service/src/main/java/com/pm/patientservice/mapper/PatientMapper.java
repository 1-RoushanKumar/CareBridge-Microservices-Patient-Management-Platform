package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient) {
        return PatientResponseDTO.builder()
                .id(patient.getId().toString())
                .name(patient.getName())
                .address(patient.getAddress())
                .email(patient.getEmail())
                .dateOfBirth(patient.getDateOfBirth().toString())
                .registeredDate(patient.getRegisteredDate().toString())
                .contact(patient.getContact())
                .gender(patient.getGender())
                .emergencyContact(patient.getEmergencyContact())
                .build();
    }

    public static Patient toModel(PatientRequestDTO patientRequestDTO) {
        Patient patient = new Patient();
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setContact(patientRequestDTO.getContact());
        patient.setGender(patientRequestDTO.getGender());
        patient.setEmergencyContact(patientRequestDTO.getEmergencyContact());
        patient.setRegisteredDate(LocalDate.now());

        return patient;
    }

}