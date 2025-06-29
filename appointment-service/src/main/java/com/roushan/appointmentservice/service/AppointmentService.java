// src/main/java/com/roushan/appointmentservice/service/AppointmentService.java
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.function.Function;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientDetailsRepository patientDetailsRepository;
    private final WebClient doctorServiceWebClient;

    public AppointmentService(AppointmentRepository appointmentRepository, PatientDetailsRepository patientDetailsRepository, WebClient doctorServiceWebClient) {
        this.appointmentRepository = appointmentRepository;
        this.patientDetailsRepository = patientDetailsRepository;
        this.doctorServiceWebClient = doctorServiceWebClient;
    }

    @Transactional
    public AppointmentResponseDTO bookAppointment(UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        UUID patientIdToBook;

        // Determine the patient ID for the appointment based on user role
        if (isAdmin) {
            if (requestDTO.getPatientId() == null || requestDTO.getPatientId().isEmpty()) {
                throw new IllegalArgumentException("For ADMIN role, 'patientId' must be provided in the request body to book an appointment.");
            }
            patientIdToBook = UUID.fromString(requestDTO.getPatientId());
        } else {
            if (requestDTO.getPatientId() != null && !requestDTO.getPatientId().isEmpty() &&
                !UUID.fromString(requestDTO.getPatientId()).equals(requestingUserId)) {
                throw new SecurityException("Patients are not authorized to book appointments for other patients. Please omit 'patientId' or ensure it matches your own.");
            }
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
        if (appointmentTime.isBefore(LocalDateTime.now().plusMinutes(15))) {
            throw new IllegalArgumentException("Appointment date and time cannot be in the past or too soon. Please provide a future time at least 15 minutes from now.");
        }

        UUID doctorId;
        try {
            doctorId = UUID.fromString(requestDTO.getDoctorId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Doctor ID format. Must be a valid UUID.", e);
        }

        if (requestDTO.getDoctorSlotId() == null) {
            throw new IllegalArgumentException("Doctor slot ID must be provided to book an appointment.");
        }
        UUID doctorSlotId = UUID.fromString(requestDTO.getDoctorSlotId());

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientIdToBook);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDateTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setDoctorSlotId(doctorSlotId);

        Appointment savedInitialAppointment = appointmentRepository.save(appointment);
        UUID generatedAppointmentId = savedInitialAppointment.getId();

        try {
            doctorServiceWebClient.put()
                    .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/book")
                            .queryParam("doctorId", doctorId)
                            .queryParam("appointmentId", generatedAppointmentId)
                            .build(doctorSlotId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(Map.class)
                                    .flatMap((Function<Map, Mono<? extends Throwable>>) body -> {
                                        String message = (String) body.getOrDefault("message", "Unknown error");
                                        return Mono.error(new IllegalArgumentException("Doctor Service error: " + message));
                                    })
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new RuntimeException("Doctor Service internal error during slot booking."))
                    )
                    .bodyToMono(Object.class)
                    .block();

            savedInitialAppointment.setStatus(AppointmentStatus.SCHEDULED);

        } catch (WebClientResponseException e) {
            savedInitialAppointment.setStatus(AppointmentStatus.FAILED);
            appointmentRepository.save(savedInitialAppointment);
            throw new IllegalArgumentException("Failed to book doctor slot: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            savedInitialAppointment.setStatus(AppointmentStatus.FAILED);
            appointmentRepository.save(savedInitialAppointment);
            throw new RuntimeException("Error communicating with Doctor Service: " + e.getMessage(), e);
        }

        Appointment finalSavedAppointment = appointmentRepository.save(savedInitialAppointment);
        return AppointmentMapper.toDTO(finalSavedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(UUID appointmentId, UUID requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to cancel this appointment. Only the patient who booked it or an ADMIN can cancel.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment. Status is " + appointment.getStatus().name().toLowerCase() + ".");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Appointment with ID '" + appointmentId + "' is already canceled.");
        }
        if (appointment.getAppointmentDateTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Appointments cannot be canceled within 1 hour of the scheduled time.");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        if (updatedAppointment.getDoctorId() != null && updatedAppointment.getDoctorSlotId() != null) {
            try {
                doctorServiceWebClient.put()
                        .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/unbook") // Changed path for clarity/consistency
                                .queryParam("doctorId", updatedAppointment.getDoctorId())
                                .queryParam("appointmentId", updatedAppointment.getId())
                                .build(updatedAppointment.getDoctorSlotId()))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response ->
                                response.bodyToMono(Map.class)
                                        .flatMap((Function<Map, Mono<? extends Throwable>>) body -> {
                                            String message = (String) body.getOrDefault("message", "Unknown error");
                                            return Mono.error(new IllegalArgumentException("Doctor Service error unbooking slot: " + message));
                                        })
                        )
                        .onStatus(HttpStatusCode::is5xxServerError, response ->
                                Mono.error(new RuntimeException("Doctor Service internal error during slot unbooking."))
                        )
                        .bodyToMono(Object.class)
                        .block();
            } catch (WebClientResponseException e) {
                System.err.println("WARNING: Appointment cancelled, but failed to unbook slot in Doctor Service. Slot ID: " + updatedAppointment.getDoctorSlotId() + ". Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("WARNING: Error communicating with Doctor Service during slot unbooking. Slot ID: " + updatedAppointment.getDoctorSlotId() + ". Error: " + e.getMessage());
            }
        }
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

        UUID oldDoctorId = appointment.getDoctorId();
        UUID oldDoctorSlotId = appointment.getDoctorSlotId();
        LocalDateTime oldAppointmentDateTime = appointment.getAppointmentDateTime();

        UUID newDoctorId;
        if (requestDTO.getDoctorId() != null && !requestDTO.getDoctorId().isEmpty()) {
            try {
                newDoctorId = UUID.fromString(requestDTO.getDoctorId());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Doctor ID format for reschedule. Must be a valid UUID.", e);
            }
        } else {
            newDoctorId = oldDoctorId; // If new doctor not specified, keep the same doctor
        }

        if (requestDTO.getDoctorSlotId() == null) {
            throw new IllegalArgumentException("New doctor slot ID must be provided to reschedule an appointment.");
        }
        UUID newDoctorSlotId = UUID.fromString(requestDTO.getDoctorSlotId());

        // --- RESCHEDULING LOGIC ---
        // Check if the slot or doctor is actually changing, OR if the time is changing (even if slot/doctor are same)
        // We always try to 're-book' the new slot to ensure the Doctor Service is aware of the updated appointment time
        // or the change in slot/doctor, leveraging the enhanced markSlotBooked for idempotency.
        boolean isSlotOrDoctorChanging = !(newDoctorSlotId.equals(oldDoctorSlotId) && newDoctorId.equals(oldDoctorId));
        boolean isTimeChanging = !newAppointmentTime.equals(oldAppointmentDateTime);

        // We will *always* attempt to book the new slot with the existing appointmentId.
        // The Doctor Service's `markSlotBooked` is now smart enough to handle
        // re-booking the same slot by the same appointment ID.
        try {
            doctorServiceWebClient.put()
                    .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/book")
                            .queryParam("doctorId", newDoctorId)
                            .queryParam("appointmentId", appointmentId) // Pass existing appointment ID
                            .build(newDoctorSlotId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(Map.class)
                                    .flatMap((Function<Map, Mono<? extends Throwable>>) body -> {
                                        String message = (String) body.getOrDefault("message", "Unknown error");
                                        return Mono.error(new IllegalArgumentException("Doctor Service error booking new slot for reschedule: " + message));
                                    })
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new RuntimeException("Doctor Service internal error during new slot booking for reschedule."))
                    )
                    .bodyToMono(Object.class)
                    .block();

            // If the new slot booking succeeded (or was a no-op due to same slot/appt ID),
            // then proceed to unbook the old one IF it's a different slot.
            if (oldDoctorSlotId != null && !oldDoctorSlotId.equals(newDoctorSlotId)) {
                try {
                    doctorServiceWebClient.put()
                            .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/unbook")
                                    .queryParam("doctorId", oldDoctorId)
                                    .queryParam("appointmentId", appointmentId)
                                    .build(oldDoctorSlotId))
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, response ->
                                    response.bodyToMono(Map.class)
                                            .flatMap((Function<Map, Mono<? extends Throwable>>) body -> {
                                                String message = (String) body.getOrDefault("message", "Unknown error");
                                                return Mono.error(new IllegalArgumentException("Doctor Service error unbooking old slot: " + message));
                                            })
                            )
                            .onStatus(HttpStatusCode::is5xxServerError, response ->
                                    Mono.error(new RuntimeException("Doctor Service internal error during old slot unbooking."))
                            )
                            .bodyToMono(Object.class)
                            .block();
                } catch (WebClientResponseException e) {
                    System.err.println("WARNING: Appointment rescheduled, but failed to unbook OLD slot in Doctor Service. Old Slot ID: " + oldDoctorSlotId + ". Error: " + e.getMessage());
                    // Don't re-throw here. The new slot is booked and local appointment is updated.
                    // This indicates a potential inconsistency that needs reconciliation in Doctor Service.
                } catch (Exception e) {
                    System.err.println("WARNING: Error communicating with Doctor Service during OLD slot unbooking. Old Slot ID: " + oldDoctorSlotId + ". Error: " + e.getMessage());
                    // Same as above, don't re-throw.
                }
            }
        } catch (WebClientResponseException e) {
            // This catches errors from the *new slot booking* attempt
            throw new IllegalArgumentException("Failed to book new doctor slot for reschedule: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            // This catches general communication errors for the *new slot booking* attempt
            throw new RuntimeException("Error communicating with Doctor Service during new slot booking for reschedule: " + e.getMessage(), e);
        }


        // Update appointment details in your service
        appointment.setAppointmentDateTime(newAppointmentTime);
        appointment.setDoctorId(newDoctorId);
        appointment.setDoctorSlotId(newDoctorSlotId);
        appointment.setStatus(AppointmentStatus.RESCHEDULED); // Set to RESCHEDULED status

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
            final UUID finalDoctorIdFromToken = doctorIdFromToken;
            try {
                doctorServiceWebClient.get()
                        .uri("/doctors/{id}", finalDoctorIdFromToken)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response ->
                                Mono.error(new ResourceNotFoundException("Doctor not found or inactive for ID: " + finalDoctorIdFromToken))
                        )
                        .onStatus(HttpStatusCode::is5xxServerError, response ->
                                Mono.error(new RuntimeException("Doctor Service internal error checking doctor status."))
                        )
                        .bodyToMono(Object.class)
                        .block();
            } catch (WebClientResponseException e) {
                throw new ResourceNotFoundException("Doctor not found or inactive with ID: " + finalDoctorIdFromToken, e);
            } catch (Exception e) {
                throw new RuntimeException("Error communicating with Doctor Service to verify doctor: " + e.getMessage(), e);
            }
            appointments = appointmentRepository.findByDoctorId(finalDoctorIdFromToken);
        } else {
            throw new SecurityException("Unauthorized access: Cannot retrieve appointments without patient or doctor identity in token.");
        }
        return appointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELED && a.getStatus() != AppointmentStatus.FAILED) // Filter out canceled/failed for general listing
                .map(AppointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID patientIdFromToken, UUID doctorIdFromToken, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (isAdmin) {
            return AppointmentMapper.toDTO(appointment);
        } else if (patientIdFromToken != null && appointment.getPatientId().equals(patientIdFromToken)) {
            return AppointmentMapper.toDTO(appointment);
        } else if (doctorIdFromToken != null && appointment.getDoctorId().equals(doctorIdFromToken)) {
            final UUID finalDoctorIdFromToken = doctorIdFromToken;
            try {
                doctorServiceWebClient.get()
                        .uri("/doctors/{id}", finalDoctorIdFromToken)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response ->
                                Mono.error(new ResourceNotFoundException("Doctor not found or inactive for ID: " + finalDoctorIdFromToken))
                        )
                        .onStatus(HttpStatusCode::is5xxServerError, response ->
                                Mono.error(new RuntimeException("Doctor Service internal error checking doctor status."))
                        )
                        .bodyToMono(Object.class)
                        .block();
            } catch (WebClientResponseException e) {
                throw new SecurityException("You are not authorized to view this appointment as the doctor is not found or inactive.", e);
            } catch (Exception e) {
                throw new RuntimeException("Error communicating with Doctor Service to verify doctor: " + e.getMessage(), e);
            }
            return AppointmentMapper.toDTO(appointment);
        } else {
            throw new SecurityException("You are not authorized to view this appointment. Access denied.");
        }
    }
}