package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PatientService {

    private final KafkaProducer kafkaProducer;
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
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
            throw new EmailAlreadyExistsException("A patient with this email already exists: " + patientRequestDTO.getEmail());
        }

        Patient patient = PatientMapper.toModel(patientRequestDTO);

        Patient newPatient = patientRepository.save(patient);

        billingServiceGrpcClient.createBillingAccount(String.valueOf(newPatient.getId()), newPatient.getName(), newPatient.getEmail());
        kafkaProducer.sendEvent(newPatient);

        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO, Authentication auth) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));

        // The @PreAuthorize on the controller method will handle most of this logic.
        // However, keeping this internal check is not harmful, but potentially redundant
        // if the @PreAuthorize expression is robust.
        // If the @PreAuthorize is: @PreAuthorize("hasRole('ADMIN') or (hasRole('PATIENT') and @patientService.isOwner(#id, authentication.name))")
        // then this internal check might not be strictly necessary, but it acts as a safeguard.
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isSameUser = patient.getEmail().equals(auth.getName());

        if (!isAdmin && !isSameUser) {
            // This throw will be caught by Spring Security's AccessDeniedHandler
            throw new org.springframework.security.access.AccessDeniedException("You can only update your own profile or you need ADMIN role.");
        }

        boolean emailTakenByAnother = patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id);
        if (emailTakenByAnother) {
            throw new EmailAlreadyExistsException("Another patient with this email already exists: " + patientRequestDTO.getEmail());
        }

        // Update allowed fields
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

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
        return PatientMapper.toDTO(patient);
    }

    /**
     * Helper method to check if the current authenticated user is the owner of the patient resource.
     * This method is called by Spring Security's @PreAuthorize annotation.
     *
     * @param patientId The UUID of the patient resource.
     * @param currentUserEmail The email of the currently authenticated user (from JWT subject).
     * @return true if the current user is the owner, false otherwise.
     */
    public boolean isOwner(UUID patientId, String currentUserEmail) {
        // Find the patient by ID
        return patientRepository.findById(patientId)
                .map(patient -> patient.getEmail().equals(currentUserEmail)) // Check if the patient's email matches the current user's email
                .orElse(false); // If patient not found, current user is not the owner
    }
}