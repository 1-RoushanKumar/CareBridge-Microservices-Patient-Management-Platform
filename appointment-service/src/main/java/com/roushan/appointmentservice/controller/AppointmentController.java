package com.roushan.appointmentservice.controller;

import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    private boolean isAdminUser() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Operation(summary = "Book a new appointment")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_PATIENT') and #requestDTO.patientId == null) or (hasRole('ROLE_PATIENT') and #requestDTO.patientId != null and #requestDTO.patientId.equals(@jwtUtil.extractPatientId(#request.getHeader('Authorization').substring(7)).toString()))")
    public ResponseEntity<AppointmentResponseDTO> bookAppointment(
            @Valid @RequestBody AppointmentRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        boolean isAdmin = isAdminUser();

        // The service layer should handle patientId validation (if admin, check requestDTO's patientId; if patient, use token's patientId).
        AppointmentResponseDTO responseDTO = appointmentService.bookAppointment(patientIdFromToken, requestDTO, isAdmin);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Cancel an existing appointment")
    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @PathVariable UUID appointmentId,
            HttpServletRequest request) {

        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        boolean isAdmin = isAdminUser();

        AppointmentResponseDTO dto = appointmentService.cancelAppointment(appointmentId, patientIdFromToken, isAdmin);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Reschedule an existing appointment")
    @PutMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        boolean isAdmin = isAdminUser();

        AppointmentResponseDTO responseDTO =
                appointmentService.rescheduleAppointment(appointmentId, patientIdFromToken, requestDTO, isAdmin);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Get all appointments (Admin/Doctor/Patient specific)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointments(HttpServletRequest request) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        UUID doctorIdFromToken = (UUID) request.getAttribute("doctorId"); // Assuming doctorId also extracted by filter
        boolean isAdmin = isAdminUser();

        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments(patientIdFromToken, doctorIdFromToken, isAdmin);
        return ResponseEntity.ok(appointments);
    }

    @Operation(summary = "Get appointment details by ID")
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(
            @PathVariable UUID appointmentId,
            HttpServletRequest request
    ) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        UUID doctorIdFromToken = (UUID) request.getAttribute("doctorId");
        boolean isAdmin = isAdminUser();

        // The service method should contain the logic to check authorization
        AppointmentResponseDTO dto = appointmentService.getAppointmentById(appointmentId, patientIdFromToken, doctorIdFromToken, isAdmin);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Mark an existing appointment as completed")
    @PutMapping("/{appointmentId}/complete")
    // Only Admin or the associated Doctor can complete an appointment
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_DOCTOR')")
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(
            @PathVariable UUID appointmentId,
            HttpServletRequest request) {

        UUID doctorIdFromToken = (UUID) request.getAttribute("doctorId"); // Get doctorId from token
        boolean isAdmin = isAdminUser();

        // Pass doctorIdFromToken to the service for internal authorization logic
        AppointmentResponseDTO dto = appointmentService.completeAppointment(appointmentId, doctorIdFromToken, isAdmin);
        return ResponseEntity.ok(dto);
    }
}