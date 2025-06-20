package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API For Managing Patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(
            summary = "Get patients with optional search, filter, and pagination",
            description = "Retrieve a list of patients, optionally filtered by name or email, with pagination and sorting support."
    )
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can list all patients
    public ResponseEntity<Page<PatientResponseDTO>> getPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @Parameter(description = "Pagination and sorting information")
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<PatientResponseDTO> patients = patientService.getPatients(name, email, pageable);
        return ResponseEntity.ok().body(patients);
    }

    // Example: Get patient by ID - ADMIN can get any, PATIENT can only get their own
    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PATIENT') and @patientService.isOwner(#id, authentication.name))")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id, Authentication authentication) {
        // You'd need to implement isOwner method in PatientService
        // Example: PatientService.isOwner(UUID patientId, String currentUserEmail)
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok().body(patient);
    }

    // Example: Only ADMINs can create patients
    @PostMapping
    @Operation(summary = "Create a new patient")
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can create new patients
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class})
            @RequestBody PatientRequestDTO patientRequestDTO) {
        PatientResponseDTO responseDTO = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a patient")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                            @RequestBody PatientRequestDTO dto,
                                                            Authentication auth) {
        return ResponseEntity.ok(patientService.updatePatient(id, dto, auth));
    }

    // Example: Only ADMINs can delete patients
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Patient")
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can delete patients
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

}
