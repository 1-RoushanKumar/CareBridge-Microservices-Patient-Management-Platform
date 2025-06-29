// src/main/java/com/roushan/doctorservice/service/DoctorService.java
package com.roushan.doctorservice.service;

import com.roushan.doctorservice.dtos.AvailabilityRequestDTO;
import com.roushan.doctorservice.dtos.DoctorRequestDTO;
import com.roushan.doctorservice.dtos.DoctorResponseDTO;
import com.roushan.doctorservice.dtos.DoctorSlotDTO;
import com.roushan.doctorservice.exception.ResourceNotFoundException;
import com.roushan.doctorservice.mapper.DoctorMapper;
import com.roushan.doctorservice.mapper.DoctorSlotMapper;
import com.roushan.doctorservice.model.Doctor;
import com.roushan.doctorservice.model.DoctorSlot;
import com.roushan.doctorservice.model.DoctorStatus;
import com.roushan.doctorservice.repository.DoctorRepository;
import com.roushan.doctorservice.repository.DoctorSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    // Add a logger instance
    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository doctorSlotRepository;

    public DoctorService(DoctorRepository doctorRepository, DoctorSlotRepository doctorSlotRepository) {
        this.doctorRepository = doctorRepository;
        this.doctorSlotRepository = doctorSlotRepository;
    }

    @Transactional
    public DoctorResponseDTO createDoctor(DoctorRequestDTO requestDTO) {
        Doctor doctor = DoctorMapper.toEntity(requestDTO);
        doctor.setStatus(DoctorStatus.ACTIVE); // New doctors are active by default
        Doctor savedDoctor = doctorRepository.save(doctor);
        return DoctorMapper.toDTO(savedDoctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponseDTO getDoctorById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
        return DoctorMapper.toDTO(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponseDTO> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(DoctorMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DoctorResponseDTO updateDoctor(UUID id, DoctorRequestDTO requestDTO) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));

        // Update fields from DTO
        doctor.setFirstName(requestDTO.getFirstName());
        doctor.setLastName(requestDTO.getLastName());
        doctor.setSpecialization(requestDTO.getSpecialization());
        doctor.setEmail(requestDTO.getEmail());
        doctor.setPhoneNumber(requestDTO.getPhoneNumber());
        doctor.setLicenseNumber(requestDTO.getLicenseNumber());
        doctor.setExperienceYears(requestDTO.getExperienceYears());
        doctor.setClinicAddress(requestDTO.getClinicAddress());
        doctor.setStatus(requestDTO.getStatus());

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return DoctorMapper.toDTO(updatedDoctor);
    }

    @Transactional
    public void deleteDoctor(UUID id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Doctor not found with ID: " + id);
        }
        doctorRepository.deleteById(id);
    }

    /**
     * Admin/Doctor can manually create slots for a doctor.
     * This is useful for specific additions or overriding generated schedules.
     */
    @Transactional
    public DoctorSlotDTO createDoctorSlot(UUID doctorId, DoctorSlotDTO slotDTO) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (!slotDTO.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("Doctor ID in path and body must match.");
        }

        // Basic validation: ensure end time is after start time
        if (slotDTO.getStartTime().isAfter(slotDTO.getEndTime()) || slotDTO.getStartTime().isEqual(slotDTO.getEndTime())) {
            throw new IllegalArgumentException("Slot end time must be after start time.");
        }

        // Check for overlapping slots for the same doctor
        // This is important to prevent double booking in the doctor service itself.
        // For simplicity, we are checking for direct overlaps. More complex logic might be needed.
        List<DoctorSlot> overlappingSlots = doctorSlotRepository.findByDoctorIdAndStartTimeBetweenAndIsBookedFalse(
                doctorId, slotDTO.getStartTime().minusMinutes(1), slotDTO.getEndTime().plusMinutes(1)
        );
        if (!overlappingSlots.isEmpty()) {
            // Further refinement needed: check if overlap is exact or partial
            boolean actualOverlap = overlappingSlots.stream().anyMatch(existingSlot ->
                    (slotDTO.getStartTime().isBefore(existingSlot.getEndTime()) && slotDTO.getEndTime().isAfter(existingSlot.getStartTime()))
            );
            if (actualOverlap) {
                throw new IllegalArgumentException("The requested slot overlaps with an existing slot for this doctor.");
            }
        }


        DoctorSlot slot = DoctorSlotMapper.toEntity(slotDTO);
        slot.setBooked(false); // New slots are always created as available
        DoctorSlot savedSlot = doctorSlotRepository.save(slot);
        return DoctorSlotMapper.toDTO(savedSlot);
    }

    /**
     * Gets available slots for appointment booking.
     * Can query by specific doctor or by specialization.
     */
    @Transactional(readOnly = true)
    public List<DoctorSlotDTO> getAvailableSlots(AvailabilityRequestDTO requestDTO) {
        List<DoctorSlot> availableSlots;
        LocalDateTime startOfDay = requestDTO.getDate().atStartOfDay();
        LocalDateTime endOfDay = requestDTO.getDate().atTime(LocalTime.MAX);

        if (requestDTO.getDoctorId() != null) {
            // Check availability for a specific doctor
            Doctor doctor = doctorRepository.findById(requestDTO.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + requestDTO.getDoctorId()));
            if (doctor.getStatus() != DoctorStatus.ACTIVE) {
                throw new IllegalArgumentException("Doctor with ID " + doctor.getId() + " is not active and cannot accept appointments.");
            }
            availableSlots = doctorSlotRepository.findByDoctorIdAndStartTimeBetweenAndIsBookedFalse(
                    requestDTO.getDoctorId(), startOfDay, endOfDay);

        } else if (requestDTO.getSpecialization() != null && !requestDTO.getSpecialization().isEmpty()) {
            // Find all active doctors for a given specialization
            List<Doctor> doctors = doctorRepository.findBySpecializationAndStatus(requestDTO.getSpecialization(), DoctorStatus.ACTIVE);
            if (doctors.isEmpty()) {
                return List.of(); // No doctors found for this specialization
            }
            Set<UUID> doctorIds = doctors.stream().map(Doctor::getId).collect(Collectors.toSet());
            availableSlots = doctorSlotRepository.findAvailableSlotsForDoctorsInTimeRange(
                    List.copyOf(doctorIds), startOfDay, endOfDay);
        } else {
            // No specific doctor or specialization: return all available slots for active doctors
            List<Doctor> activeDoctors = doctorRepository.findByStatus(DoctorStatus.ACTIVE);
            if (activeDoctors.isEmpty()) {
                return List.of();
            }
            Set<UUID> activeDoctorIds = activeDoctors.stream().map(Doctor::getId).collect(Collectors.toSet());
            availableSlots = doctorSlotRepository.findAvailableSlotsForDoctorsInTimeRange(
                    List.copyOf(activeDoctorIds), startOfDay, endOfDay);
        }

        // Filter out any slots that are already in the past relative to now (important for long-running processes)
        LocalDateTime now = LocalDateTime.now();
        return availableSlots.stream()
                .filter(slot -> slot.getStartTime().isAfter(now))
                .map(DoctorSlotMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marks a doctor slot as booked. This is called by Appointment Service.
     */
    @Transactional
    public DoctorSlotDTO markSlotBooked(UUID slotId, UUID doctorId, UUID appointmentId) {
        log.info("Attempting to mark slot as booked. Slot ID: {}, Doctor ID: {}, Appointment ID: {}", slotId, doctorId, appointmentId);

        try {
            DoctorSlot slot = doctorSlotRepository.findByIdAndDoctorId(slotId, doctorId)
                    .orElseThrow(() -> {
                        log.warn("Doctor slot not found for booking. Slot ID: {}, Doctor ID: {}. This might indicate an invalid request.", slotId, doctorId);
                        return new ResourceNotFoundException("Doctor slot not found with ID: " + slotId + " for doctor: " + doctorId);
                    });

            // --- CRUCIAL CHANGE FOR RESCHEDULING ---
            if (slot.isBooked()) {
                if (appointmentId.equals(slot.getAppointmentId())) {
                    // This slot is already booked by the *same* appointment.
                    // This can happen during a reschedule if the slot itself hasn't changed.
                    // Treat as success (idempotent operation for the same appointment).
                    log.info("Slot {} is already booked by the same appointment ID {}. Treating as successful re-booking.", slotId, appointmentId);
                    return DoctorSlotMapper.toDTO(slot); // Return the current state
                } else {
                    // Slot is booked by a *different* appointment. This is a genuine conflict.
                    log.warn("Attempt to book an already booked slot {} by appointment ID {}. It is currently booked by a different appointment ID {}.", slotId, appointmentId, slot.getAppointmentId());
                    throw new IllegalArgumentException("Slot with ID " + slotId + " is already booked by another appointment.");
                }
            }

            // If not booked, proceed with booking
            slot.setBooked(true);
            slot.setAppointmentId(appointmentId);
            DoctorSlot updatedSlot = doctorSlotRepository.save(slot);
            log.info("Successfully marked slot {} as booked for appointment {}", slotId, appointmentId);
            return DoctorSlotMapper.toDTO(updatedSlot);
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Business rule violation or resource not found during slot booking for slot ID {}: {}", slotId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An unexpected technical error occurred while marking slot {} as booked: {}", slotId, e.getMessage(), e);
            throw new RuntimeException("An unexpected internal error occurred in Doctor Service while booking slot.", e);
        }
    }

    /**
     * Marks a doctor slot as unbooked (e.g., after appointment cancellation).
     * Needs to verify the appointmentId to prevent unauthorized unbooking.
     */
    @Transactional
    public DoctorSlotDTO markSlotUnbooked(UUID slotId, UUID doctorId, UUID appointmentId) {
        DoctorSlot slot = doctorSlotRepository.findByIdAndDoctorId(slotId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor slot not found with ID: " + slotId + " for doctor: " + doctorId));

        if (!slot.isBooked()) {
            throw new IllegalArgumentException("Slot with ID " + slotId + " is already available (not booked).");
        }

        // Crucial: Only unbook if the request comes from the original appointment
        if (slot.getAppointmentId() == null || !slot.getAppointmentId().equals(appointmentId)) {
            throw new SecurityException("Unauthorized attempt to unbook slot: Mismatching appointment ID or slot not linked to an appointment.");
        }

        slot.setBooked(false);
        slot.setAppointmentId(null); // Clear the appointment ID
        DoctorSlot updatedSlot = doctorSlotRepository.save(slot);
        return DoctorSlotMapper.toDTO(updatedSlot);
    }

    // --- Utility to generate slots (e.g., daily or weekly) ---
    // This could be triggered by an admin, or a scheduled job.
    @Transactional
    public List<DoctorSlotDTO> generateSlotsForDoctor(UUID doctorId, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, int durationMinutes) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot generate slots for an inactive doctor.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Slot duration must be positive.");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("End time must be after start time for slot generation.");
        }

        List<DoctorSlot> generatedSlots = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime currentSlotStart = LocalDateTime.of(currentDate, startTime);
            while (currentSlotStart.plusMinutes(durationMinutes).isBefore(LocalDateTime.of(currentDate, endTime).plusMinutes(1))) {
                LocalDateTime currentSlotEnd = currentSlotStart.plusMinutes(durationMinutes);
                DoctorSlot newSlot = new DoctorSlot();
                newSlot.setDoctorId(doctorId);
                newSlot.setStartTime(currentSlotStart);
                newSlot.setEndTime(currentSlotEnd);
                newSlot.setBooked(false);
                generatedSlots.add(newSlot);
                currentSlotStart = currentSlotEnd; // Move to the next slot start
            }
            currentDate = currentDate.plusDays(1);
        }

        // Save all generated slots in a batch
        List<DoctorSlot> savedSlots = doctorSlotRepository.saveAll(generatedSlots);
        return savedSlots.stream()
                .map(DoctorSlotMapper::toDTO)
                .collect(Collectors.toList());
    }
}