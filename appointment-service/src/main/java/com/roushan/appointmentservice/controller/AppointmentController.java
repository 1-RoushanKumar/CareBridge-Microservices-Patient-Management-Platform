package com.roushan.appointmentservice.controller;

import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.exception.ResourceNotFoundException;
import com.roushan.appointmentservice.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Books a new appointment.
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_PATIENT') and #requestDTO.patientId != null and #requestDTO.patientId.equals(#request.getAttribute('patientId')?.toString()))")
    public ResponseEntity<AppointmentResponseDTO> bookAppointment(
            @Valid @RequestBody AppointmentRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            AppointmentResponseDTO responseDTO = appointmentService.bookAppointment(patientIdFromToken, requestDTO, isAdmin);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Cancels an existing appointment.
     */
    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_PATIENT')")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable UUID appointmentId,
            HttpServletRequest request) {

        UUID patientId = (UUID) request.getAttribute("patientId");   // may be null for admin
        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            AppointmentResponseDTO dto =
                    appointmentService.cancelAppointment(appointmentId, patientId, isAdmin);
            return ResponseEntity.ok(dto);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Reschedules an existing appointment.
     */
    @PutMapping("/{appointmentId}/reschedule")
// Only check that user has the right role — leave identity checks to service layer
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<?> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        UUID patientId = (UUID) request.getAttribute("patientId"); // null for admin
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            AppointmentResponseDTO responseDTO =
                    appointmentService.rescheduleAppointment(appointmentId, patientId, requestDTO, isAdmin);
            return ResponseEntity.ok(responseDTO);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }




    /**
     * Views all appointments.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointments(HttpServletRequest request) {
        UUID patientIdFromToken = (UUID) request.getAttribute("patientId");
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments(patientIdFromToken, isAdmin);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Views a specific appointment by ID.
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getAppointmentById(
            @PathVariable UUID appointmentId,
            HttpServletRequest request
    ) {
        UUID patientId = (UUID) request.getAttribute("patientId");
        UUID doctorId = (UUID) request.getAttribute("doctorId");
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        try {
            AppointmentResponseDTO dto = appointmentService.getAppointmentById(appointmentId, patientId, isAdmin);

            // Manual doctor check
            if (!isAdmin && patientId == null) {
                if (doctorId == null) {
                    throw new SecurityException("Unauthorized: No patient or doctor identity found.");
                }
                // Load the appointment separately and check doctor ownership
                if (!dto.getDoctorId().equals(doctorId)) {
                    throw new SecurityException("You are not authorized to view this appointment.");
                }
            }

            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

}