package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient) {
        PatientResponseDTO patientDTO = new PatientResponseDTO();
        patientDTO.setId(patient.getId().toString());
        patientDTO.setName(patient.getName());
        patientDTO.setAddress(patient.getAddress());
        patientDTO.setEmail(patient.getEmail());
        patientDTO.setDateOfBirth(patient.getDateOfBirth().toString());
        patientDTO.setRegisteredDate(patient.getRegisteredDate().toString());
        patientDTO.setContact(patient.getContact()); // New field mapping
        patientDTO.setGender(patient.getGender()); // New field mapping
        patientDTO.setEmergencyContact(patient.getEmergencyContact()); // New field mapping
        return patientDTO;
    }

    public static Patient toModel(PatientRequestDTO patientRequestDTO) {
        Patient patient = new Patient();
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setContact(patientRequestDTO.getContact()); // New field mapping
        patient.setGender(patientRequestDTO.getGender()); // New field mapping
        patient.setEmergencyContact(patientRequestDTO.getEmergencyContact()); // New field mapping
        patient.setRegisteredDate(LocalDate.now());

        return patient;
    }

}