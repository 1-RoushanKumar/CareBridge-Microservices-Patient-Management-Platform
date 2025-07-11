package com.roushan.doctorservice.controller;

import com.roushan.doctorservice.dtos.AvailabilityRequestDTO;
import com.roushan.doctorservice.dtos.DoctorRequestDTO;
import com.roushan.doctorservice.dtos.DoctorResponseDTO;
import com.roushan.doctorservice.dtos.DoctorSlotDTO;
import com.roushan.doctorservice.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Operation(summary = "Create a new doctor (Admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DoctorResponseDTO> createDoctor(@Valid @RequestBody DoctorRequestDTO requestDTO) {
        DoctorResponseDTO responseDTO = doctorService.createDoctor(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Get doctor details by ID (Admin, Doctor, Patient)")
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDTO> getDoctorById(@PathVariable UUID id) {
        DoctorResponseDTO responseDTO = doctorService.getDoctorById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Get all doctors (Admin, Doctor, Patient)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<List<DoctorResponseDTO>> getAllDoctors() {
        List<DoctorResponseDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "Update doctor details (Admin, Doctor self-service)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DOCTOR') and #id.toString() == @jwtUtil.extractDoctorId(#request.getHeader('Authorization').substring(7)).toString())")
    public ResponseEntity<DoctorResponseDTO> updateDoctor(@PathVariable UUID id, @Valid @RequestBody DoctorRequestDTO requestDTO) {
        // You'll need to pass HttpServletRequest or doctorId from token here for self-service logic
        DoctorResponseDTO responseDTO = doctorService.updateDoctor(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Delete a doctor (Admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable UUID id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create a specific doctor slot (Admin/Doctor)")
    @PostMapping("/{doctorId}/slots")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DOCTOR') and #doctorId.toString() == @jwtUtil.extractDoctorId(#request.getHeader('Authorization').substring(7)).toString())")
    public ResponseEntity<DoctorSlotDTO> createDoctorSlot(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorSlotDTO slotDTO
    ) {
        DoctorSlotDTO responseDTO = doctorService.createDoctorSlot(doctorId, slotDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Generate multiple slots for a doctor (Admin/Doctor)")
    @PostMapping("/{doctorId}/slots/generate")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DOCTOR') and #doctorId.toString() == @jwtUtil.extractDoctorId(#request.getHeader('Authorization').substring(7)).toString())")
    public ResponseEntity<List<DoctorSlotDTO>> generateDoctorSlots(
            @PathVariable UUID doctorId,
            @RequestParam @NotNull @FutureOrPresent LocalDate startDate,
            @RequestParam @NotNull @FutureOrPresent LocalDate endDate,
            @RequestParam @NotNull LocalTime startTime,
            @RequestParam @NotNull LocalTime endTime,
            @RequestParam @Min(1) int durationMinutes
    ) {
        List<DoctorSlotDTO> generatedSlots = doctorService.generateSlotsForDoctor(
                doctorId, startDate, endDate, startTime, endTime, durationMinutes
        );
        return ResponseEntity.ok(generatedSlots);
    }


    @Operation(summary = "Get available doctor slots for a date/specialization (Public for Appointment Service)")
    @GetMapping("/slots/available")
    public ResponseEntity<List<DoctorSlotDTO>> getAvailableSlots(@Valid @ModelAttribute AvailabilityRequestDTO requestDTO) {
        // @ModelAttribute is used for GET requests with complex objects
        List<DoctorSlotDTO> slots = doctorService.getAvailableSlots(requestDTO);
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Mark a doctor slot as booked (Internal: Appointment Service calls this)")
    @PutMapping("/slots/{slotId}/book")
    @PreAuthorize("hasRole('ROLE_APPOINTMENT_SERVICE')") // Define a custom role for internal service calls
    public ResponseEntity<DoctorSlotDTO> markSlotBooked(
            @PathVariable UUID slotId,
            @RequestParam UUID doctorId, // Doctor ID should be passed to confirm ownership
            @RequestParam UUID appointmentId // Link to the appointment
    ) {
        DoctorSlotDTO responseDTO = doctorService.markSlotBooked(slotId, doctorId, appointmentId);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Mark a doctor slot as unbooked (Internal: Appointment Service calls this on cancellation)")
    @PutMapping("/slots/{slotId}/unbook")
    @PreAuthorize("hasRole('ROLE_APPOINTMENT_SERVICE')") // Define a custom role for internal service calls
    public ResponseEntity<DoctorSlotDTO> markSlotUnbooked(
            @PathVariable UUID slotId,
            @RequestParam UUID doctorId,
            @RequestParam UUID appointmentId // Ensure it's the correct appointment unbooking it
    ) {
        DoctorSlotDTO responseDTO = doctorService.markSlotUnbooked(slotId, doctorId, appointmentId);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Get a doctor's consultation fee by ID")
    @GetMapping("/{doctorId}/fee")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SERVICE')") // Define 'ROLE_SERVICE' for internal calls
    public ResponseEntity<Double> getDoctorFee(@PathVariable UUID doctorId) {
        double fee = doctorService.getDoctorConsultationFee(doctorId);
        return ResponseEntity.ok(fee);
    }
}