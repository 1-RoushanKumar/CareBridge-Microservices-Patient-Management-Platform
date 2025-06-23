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

    public AppointmentService(AppointmentRepository appointmentRepository, PatientDetailsRepository patientDetailsRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientDetailsRepository = patientDetailsRepository;
    }

    @Transactional
    public AppointmentResponseDTO bookAppointment(UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        UUID patientIdToBook;

        // Determine the patient ID for the appointment based on user role
        if (isAdmin) {
            // Admin can book for any patient, so patientId is required in DTO
            if (requestDTO.getPatientId() == null || requestDTO.getPatientId().isEmpty()) {
                throw new IllegalArgumentException("For ADMIN role, 'patientId' must be provided in the request body to book an appointment.");
            }
            patientIdToBook = UUID.fromString(requestDTO.getPatientId());
        } else {
            // Non-admin users (e.g., PATIENT) can only book for themselves
            if (requestDTO.getPatientId() != null && !requestDTO.getPatientId().isEmpty() &&
                !UUID.fromString(requestDTO.getPatientId()).equals(requestingUserId)) {
                throw new SecurityException("Patients are not authorized to book appointments for other patients. Please omit 'patientId' or ensure it matches your own.");
            }
            // Use the patient ID from the authenticated token
            if (requestingUserId == null) {
                throw new SecurityException("Authenticated patient ID not found in token. Cannot book appointment.");
            }
            patientIdToBook = requestingUserId;
        }

        // Validate patient existence and status
        PatientDetails patient = patientDetailsRepository.findById(patientIdToBook)
                .orElseThrow(() -> new ResourceNotFoundException("Patient with ID '" + patientIdToBook + "' not found in local records. Appointment cannot be booked."));

        if (!"ACTIVE".equalsIgnoreCase(patient.getStatus())) {
            throw new IllegalArgumentException("Patient with ID '" + patientIdToBook + "' is currently '" + patient.getStatus() + "'. Only ACTIVE patients can book appointments.");
        }

        // Validate appointment time
        LocalDateTime appointmentTime = LocalDateTime.parse(requestDTO.getAppointmentDateTime());
        if (appointmentTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment date and time cannot be in the past. Please provide a future time.");
        }

        // Validate doctor ID (assuming doctor details might also be stored locally or fetched from a doctor service)
        // For now, just ensure it's a valid UUID string
        UUID doctorId;
        try {
            doctorId = UUID.fromString(requestDTO.getDoctorId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Doctor ID format. Must be a valid UUID.", e);
        }
        // TODO: Add logic to verify doctor's existence if necessary

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientIdToBook);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDateTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED); // Always set to SCHEDULED upon creation

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(savedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(UUID appointmentId, UUID requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Authorization check
        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to cancel this appointment. Only the patient who booked it or an ADMIN can cancel.");
        }

        // Business logic for cancellation
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment. Status is " + appointment.getStatus().name().toLowerCase() + ".");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Appointment with ID '" + appointmentId + "' is already canceled.");
        }
        if (appointment.getAppointmentDateTime().isBefore(LocalDateTime.now().plusHours(1))) { // Example: Cannot cancel within 1 hour of appointment
            throw new IllegalArgumentException("Appointments cannot be canceled within 1 hour of the scheduled time.");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO rescheduleAppointment(UUID appointmentId, UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Authorization check
        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to reschedule this appointment. Only the patient who booked it or an ADMIN can reschedule.");
        }

        // Business logic for rescheduling
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot reschedule an appointment that is already " + appointment.getStatus().name().toLowerCase() + ".");
        }

        // Validate new appointment time
        LocalDateTime newAppointmentTime = LocalDateTime.parse(requestDTO.getAppointmentDateTime());
        if (newAppointmentTime.isBefore(LocalDateTime.now().plusMinutes(30))) { // Example: New time must be at least 30 mins in future
            throw new IllegalArgumentException("New appointment time cannot be in the past or too soon. Please provide a future time at least 30 minutes from now.");
        }

        // Update doctor ID if provided in request (admin only or for patient if allowed)
        if (requestDTO.getDoctorId() != null && !requestDTO.getDoctorId().isEmpty()) {
            UUID newDoctorId;
            try {
                newDoctorId = UUID.fromString(requestDTO.getDoctorId());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Doctor ID format for reschedule. Must be a valid UUID.", e);
            }
            appointment.setDoctorId(newDoctorId);
        }

        appointment.setAppointmentDateTime(newAppointmentTime);
        appointment.setStatus(AppointmentStatus.RESCHEDULED); // Set status to RESCHEDULED

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDTO(updatedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAllAppointments(UUID patientIdFromToken, UUID doctorIdFromToken, boolean isAdmin) {
        List<Appointment> appointments;
        if (isAdmin) {
            appointments = appointmentRepository.findAll();
        } else if (patientIdFromToken != null) {
            appointments = appointmentRepository.findByPatientId(patientIdFromToken);
        } else if (doctorIdFromToken != null) {
            // Assuming you'll add a findByDoctorId method in your repository
            appointments = appointmentRepository.findByDoctorId(doctorIdFromToken);
        } else {
            // This scenario implies a user with a non-admin role, but neither patientId nor doctorId in token.
            // This might indicate a misconfigured token or an attempt to access without proper identity.
            throw new SecurityException("Unauthorized access: Cannot retrieve appointments without patient or doctor identity in token.");
        }
        return appointments.stream()
                .map(AppointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID patientIdFromToken, UUID doctorIdFromToken, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Centralized Authorization Logic
        if (isAdmin) {
            return AppointmentMapper.toDTO(appointment); // Admins can view any appointment
        } else if (patientIdFromToken != null && appointment.getPatientId().equals(patientIdFromToken)) {
            return AppointmentMapper.toDTO(appointment); // Patients can view their own appointments
        } else if (doctorIdFromToken != null && appointment.getDoctorId().equals(doctorIdFromToken)) {
            return AppointmentMapper.toDTO(appointment); // Doctors can view their own appointments
        } else {
            // If none of the above conditions met, deny access
            throw new SecurityException("You are not authorized to view this appointment. Access denied.");
        }
    }
}