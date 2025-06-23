package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PatientService {

    private final KafkaProducer kafkaProducer;
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public Page<PatientResponseDTO> getPatients(String name, String email, Pageable pageable) {
        Specification<Patient> spec = Specification.where(null);

        if (StringUtils.hasText(name)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }

        if (StringUtils.hasText(email)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }

        Page<Patient> patientsPage = patientRepository.findAll(spec, pageable);
        return patientsPage.map(PatientMapper::toDTO);
    }

    public PatientResponseDTO getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with email '" + patientRequestDTO.getEmail() + "' already exists.");
        }

        Patient patient = PatientMapper.toModel(patientRequestDTO);

        Patient newPatient = patientRepository.save(patient);
        kafkaProducer.sendEvent(newPatient);

        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO, Authentication auth) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = patient.getEmail().equals(auth.getName());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not authorized to update this patient's profile.");
        }
        boolean emailTakenByAnother = patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id);
        if (emailTakenByAnother) {
            throw new EmailAlreadyExistsException("Email '" + patientRequestDTO.getEmail() + "' is already registered to another patient.");
        }
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setContact(patientRequestDTO.getContact());
        patient.setGender(patientRequestDTO.getGender());
        patient.setEmergencyContact(patientRequestDTO.getEmergencyContact());

        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException("Patient not found with ID: " + id);
        }
        patientRepository.deleteById(id);
    }

    public PatientResponseDTO getPatientByEmail(String email) {
        Patient patient = patientRepository.findByEmail(email);
        if (patient == null) {
            throw new PatientNotFoundException("Patient with email '" + email + "' not found.");
        }
        return PatientMapper.toDTO(patient);
    }

    public boolean isOwner(UUID patientId, String currentUserEmail) {
        return patientRepository.findById(patientId)
                .map(patient -> patient.getEmail().equals(currentUserEmail))
                .orElse(false);
    }
}