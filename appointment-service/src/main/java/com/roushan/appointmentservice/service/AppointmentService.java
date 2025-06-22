package com.roushan.appointmentservice.service;

import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.exception.ResourceNotFoundException;
import com.roushan.appointmentservice.mapper.AppointmentMapper;
import com.roushan.appointmentservice.model.Appointment;
import com.roushan.appointmentservice.model.PatientDetails;
import com.roushan.appointmentservice.model.enums.AppointmentStatus;
import com.roushan.appointmentservice.repository.AppointmentRepository;
import com.roushan.appointmentservice.repository.PatientDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientDetailsRepository patientDetailsRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, PatientDetailsRepository patientDetailsRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientDetailsRepository = patientDetailsRepository;
    }

    @Transactional
    public AppointmentResponseDTO bookAppointment(UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        UUID patientIdToBook;

        if (isAdmin) {
            if (requestDTO.getPatientId() == null || requestDTO.getPatientId().isEmpty()) {
                throw new IllegalArgumentException("Patient ID is required for admin to book on behalf of another patient.");
            }
            patientIdToBook = UUID.fromString(requestDTO.getPatientId());
        } else {
            // Regular patient can only book for themselves, the requestingUserId is their patientId
            if (requestDTO.getPatientId() != null && !requestDTO.getPatientId().isEmpty() &&
                !UUID.fromString(requestDTO.getPatientId()).equals(requestingUserId)) {
                throw new SecurityException("Patients can only book appointments for themselves.");
            }
            patientIdToBook = requestingUserId; // Use the authenticated patient's ID
        }

        // --- NEW VALIDATION STEP: Check PatientDetails from local cache ---
        Optional<PatientDetails> patient = patientDetailsRepository.findById(patientIdToBook);

        if (patient.isEmpty()) {
            throw new ResourceNotFoundException("Patient with ID " + patientIdToBook + " not found in local records. Cannot book appointment.");
        }

        // Assuming "ACTIVE" is the status for a valid patient
        if (!"ACTIVE".equalsIgnoreCase(patient.get().getStatus())) {
            // You might want a more specific exception like PatientInactiveException
            throw new IllegalArgumentException("Patient with ID " + patientIdToBook + " is not active. Current status: " + patient.get().getStatus() + ". Cannot book appointment.");
        }
        // --- END NEW VALIDATION ---

        LocalDateTime appointmentTime = LocalDateTime.parse(requestDTO.getAppointmentDateTime());
        if (appointmentTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment time cannot be in the past.");
        }

        Appointment appointment = AppointmentMapper.toModel(requestDTO);
        appointment.setPatientId(patientIdToBook);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(savedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(UUID appointmentId, UUID requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to cancel this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot cancel an appointment that is already " + appointment.getStatus().name().toLowerCase() + ".");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO rescheduleAppointment(UUID appointmentId, UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to reschedule this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot reschedule an appointment that is already " + appointment.getStatus().name().toLowerCase() + ".");
        }

        LocalDateTime newAppointmentTime = LocalDateTime.parse(requestDTO.getAppointmentDateTime());
        if (newAppointmentTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New appointment time cannot be in the past.");
        }

        appointment.setAppointmentDateTime(newAppointmentTime);
        appointment.setDoctorId(UUID.fromString(requestDTO.getDoctorId()));
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(updatedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAllAppointments(UUID requestingUserId, boolean isAdmin) {
        List<Appointment> appointments;
        if (isAdmin) {
            appointments = appointmentRepository.findAll();
        } else {
            appointments = appointmentRepository.findByPatientId(requestingUserId);
        }
        return appointments.stream()
                .map(AppointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to view this appointment.");
        }
        return AppointmentMapper.toDTO(appointment);
    }
}