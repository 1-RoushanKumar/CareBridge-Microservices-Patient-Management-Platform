package com.roushan.appointmentservice.service;

import appointment.events.AppointmentBookedEvent;
import appointment.events.AppointmentCanceledEvent;
import appointment.events.AppointmentRescheduledEvent;
import com.roushan.appointmentservice.dtos.AppointmentRequestDTO;
import com.roushan.appointmentservice.dtos.AppointmentResponseDTO;
import com.roushan.appointmentservice.exception.ResourceNotFoundException;
import com.roushan.appointmentservice.mapper.AppointmentMapper;
import com.roushan.appointmentservice.model.Appointment;
import com.roushan.appointmentservice.model.PatientDetails;
import com.roushan.appointmentservice.model.enums.AppointmentStatus;
import com.roushan.appointmentservice.repository.AppointmentRepository;
import com.roushan.appointmentservice.repository.PatientDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import appointment.events.AppointmentCompletedEvent;
import com.roushan.appointmentservice.dtos.DoctorResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.function.Function;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final PatientDetailsRepository patientDetailsRepository;
    private final WebClient doctorServiceWebClient;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientDetailsRepository patientDetailsRepository,
                              WebClient doctorServiceWebClient,
                              KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.patientDetailsRepository = patientDetailsRepository;
        this.doctorServiceWebClient = doctorServiceWebClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AppointmentResponseDTO bookAppointment(UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        UUID patientIdToBook;

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

        PatientDetails patient = patientDetailsRepository.findById(patientIdToBook)
                .orElseThrow(() -> new ResourceNotFoundException("Patient with ID '" + patientIdToBook + "' not found in local records. Appointment cannot be booked."));

        if (!"ACTIVE".equalsIgnoreCase(patient.getStatus())) {
            throw new IllegalArgumentException("Patient with ID '" + patientIdToBook + "' is currently '" + patient.getStatus() + "'. Only ACTIVE patients can book appointments.");
        }

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

        // --- NEW/UPDATED: Fetch Doctor Details for the event payload and booking logic ---
        DoctorResponseDTO doctorDetails;
        try {
            // Using the actual Doctor Service endpoint: /doctors/{id}
            doctorDetails = doctorServiceWebClient.get()
                    .uri("/doctors/{id}", doctorId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ResourceNotFoundException("Doctor not found for ID: " + doctorId));
                        }
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Doctor Service error getting doctor " + doctorId + ": " + errorBody)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new RuntimeException("Doctor Service internal error getting doctor " + doctorId))
                    )
                    .bodyToMono(DoctorResponseDTO.class) // Expecting DoctorResponseDTO
                    .block(); // Block to get the result synchronously

            if (doctorDetails == null) { // Should ideally be caught by 404, but as a safeguard
                throw new ResourceNotFoundException("Doctor details could not be fetched for ID: " + doctorId);
            }
            // Add a check for doctor status if not already done in Doctor Service's endpoint logic
            // E.g., if ("INACTIVE".equalsIgnoreCase(doctorDetails.getStatus().name())) { ... }

        } catch (WebClientResponseException e) {
            // Log the actual response body if available for better debugging
            logger.error("Failed to fetch doctor details for booking. Doctor ID: {}. Status: {}. Response: {}",
                    doctorId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch doctor details for booking: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Error communicating with Doctor Service to get doctor details for ID {}: {}", doctorId, e.getMessage(), e);
            throw new RuntimeException("Error communicating with Doctor Service to get doctor details: " + e.getMessage(), e);
        }
        // --- END Doctor Details Fetch ---

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

// --- NEW: Publish AppointmentBookedEvent to Kafka ---
        try {
            String topicName = "appointment-booked-events"; // This topic name should match in Notification Service

            AppointmentBookedEvent event = AppointmentBookedEvent.newBuilder()
                    .setAppointmentId(finalSavedAppointment.getId().toString())
                    .setPatientId(finalSavedAppointment.getPatientId().toString())
                    .setDoctorId(finalSavedAppointment.getDoctorId().toString())
                    .setAppointmentDateTime(finalSavedAppointment.getAppointmentDateTime().toString())
                    .setStatus(finalSavedAppointment.getStatus().name())
                    .setPatientName(patient.getName()) // Assuming PatientDetails has a getName() method
                    .setPatientEmail(patient.getEmail()) // Assuming PatientDetails has an getEmail() method
                    .setDoctorName(doctorDetails.getFirstName() + " " + doctorDetails.getLastName()) // Using fetched doctorDetails
                    .setDoctorSpecialization(doctorDetails.getSpecialization()) // Using fetched doctorDetails
                    .setEstimatedFeeAmount(doctorDetails.getConsultationFee()) // Using fetched doctorDetails
                    .setCurrency("INR") // Hardcoded for now; consider making this dynamic if needed
                    .setEventType("APPOINTMENT_SCHEDULED") // Specific event type
                    .setTimestamp(LocalDateTime.now().toString())
                    .build();

            // Send the Protobuf event as byte array
            kafkaTemplate.send(topicName, finalSavedAppointment.getPatientId().toString(), event.toByteArray());
            logger.info("Published Protobuf AppointmentBookedEvent for appointmentId: {}", finalSavedAppointment.getId());

        } catch (Exception e) {
            logger.error("Failed to publish Protobuf AppointmentBookedEvent for appointmentId {}: {}", finalSavedAppointment.getId(), e.getMessage(), e);
            // Decide if this failure should cause a transaction rollback.
            // For notifications, usually it's fine to log the error and continue,
            // as the core appointment booking is complete.
        }

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

        // Fetch patient and doctor details BEFORE updating the appointment status to CANCELED
        // These details are needed for the Kafka event.
        PatientDetails patient = patientDetailsRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient for appointment ID " + appointmentId + " not found."));

        DoctorResponseDTO doctorDetails = null;
        try {
            doctorDetails = doctorServiceWebClient.get()
                    .uri("/doctors/{id}", appointment.getDoctorId())
                    .retrieve()
                    .bodyToMono(DoctorResponseDTO.class)
                    .block();
        } catch (Exception e) {
            logger.warn("Could not fetch doctor details for canceled appointment {}: {}", appointmentId, e.getMessage());
            // Log and continue, as the core cancellation can still proceed.
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        if (updatedAppointment.getDoctorId() != null && updatedAppointment.getDoctorSlotId() != null) {
            try {
                doctorServiceWebClient.put()
                        .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/unbook")
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

        try {
            String topicName = "appointment-canceled-events"; // Define this in application.properties as well

            AppointmentCanceledEvent.Builder eventBuilder = AppointmentCanceledEvent.newBuilder()
                    .setAppointmentId(updatedAppointment.getId().toString())
                    .setPatientId(updatedAppointment.getPatientId().toString())
                    .setDoctorId(updatedAppointment.getDoctorId().toString())
                    .setAppointmentDateTime(updatedAppointment.getAppointmentDateTime().toString())
                    .setStatus(updatedAppointment.getStatus().name())
                    .setEventType("APPOINTMENT_CANCELED")
                    .setTimestamp(LocalDateTime.now().toString());

            if (patient != null) {
                eventBuilder.setPatientName(patient.getName())
                        .setPatientEmail(patient.getEmail());
            }
            if (doctorDetails != null) {
                eventBuilder.setDoctorName(doctorDetails.getFirstName() + " " + doctorDetails.getLastName())
                        .setDoctorSpecialization(doctorDetails.getSpecialization())
                        .setEstimatedFeeAmount(doctorDetails.getConsultationFee())
                        .setCurrency("INR"); // Assuming INR from doctorDetails
            } else {
                eventBuilder.setEstimatedFeeAmount(0.0) // Default if doctor details not available
                        .setCurrency("UNKNOWN");
            }
            // Add a cancellation reason if you have it in your DTO or logic
            eventBuilder.setCancellationReason("Patient request or Admin cancellation"); // Example hardcoded reason

            kafkaTemplate.send(topicName, updatedAppointment.getId().toString(), eventBuilder.build().toByteArray());
            logger.info("Published Protobuf AppointmentCanceledEvent for appointmentId: {}", updatedAppointment.getId());

        } catch (Exception e) {
            logger.error("Failed to publish Protobuf AppointmentCanceledEvent for appointmentId {}: {}", updatedAppointment.getId(), e.getMessage(), e);
        }
        // --- END Publish AppointmentCanceledEvent ---

        return AppointmentMapper.toDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentResponseDTO rescheduleAppointment(UUID appointmentId, UUID requestingUserId, AppointmentRequestDTO requestDTO, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && !appointment.getPatientId().equals(requestingUserId)) {
            throw new SecurityException("You are not authorized to reschedule this appointment. Only the patient who booked it or an ADMIN can reschedule.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot reschedule an appointment that is already " + appointment.getStatus().name().toLowerCase() + ".");
        }

        LocalDateTime newAppointmentTime = LocalDateTime.parse(requestDTO.getAppointmentDateTime());
        if (newAppointmentTime.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("New appointment time cannot be in the past or too soon. Please provide a future time at least 30 minutes from now.");
        }

        // Store old details BEFORE modification for the event payload
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
            newDoctorId = oldDoctorId;
        }

        if (requestDTO.getDoctorSlotId() == null) {
            throw new IllegalArgumentException("New doctor slot ID must be provided to reschedule an appointment.");
        }
        UUID newDoctorSlotId = UUID.fromString(requestDTO.getDoctorSlotId());

        boolean isSlotOrDoctorChanging = !(newDoctorSlotId.equals(oldDoctorSlotId) && newDoctorId.equals(oldDoctorId));
        boolean isTimeChanging = !newAppointmentTime.equals(oldAppointmentDateTime);

        // Fetch patient and doctor details for the event payload
        PatientDetails patient = patientDetailsRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient for appointment ID " + appointmentId + " not found."));

        // NEW: Fetch OLD Doctor Details for the event payload
        DoctorResponseDTO oldDoctorDetails = null;
        try {
            oldDoctorDetails = doctorServiceWebClient.get()
                    .uri("/doctors/{id}", oldDoctorId)
                    .retrieve()
                    .bodyToMono(DoctorResponseDTO.class)
                    .block();
        } catch (Exception e) {
            logger.warn("Could not fetch old doctor details for rescheduled appointment {}: {}", appointmentId, e.getMessage());
            // Log and continue, as the core reschedule can still proceed.
        }

        DoctorResponseDTO newDoctorDetails = null; // Doctor details for the NEW doctor/slot
        try {
            newDoctorDetails = doctorServiceWebClient.get()
                    .uri("/doctors/{id}", newDoctorId)
                    .retrieve()
                    .bodyToMono(DoctorResponseDTO.class)
                    .block();
        } catch (Exception e) {
            logger.warn("Could not fetch new doctor details for rescheduled appointment {}: {}", appointmentId, e.getMessage());
            // Log and continue, as the core reschedule can still proceed.
        }


        try {
            doctorServiceWebClient.put()
                    .uri(uriBuilder -> uriBuilder.path("/doctors/slots/{slotId}/book")
                            .queryParam("doctorId", newDoctorId)
                            .queryParam("appointmentId", appointmentId)
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
                } catch (Exception e) {
                    System.err.println("WARNING: Error communicating with Doctor Service during OLD slot unbooking. Old Slot ID: " + oldDoctorSlotId + ". Error: " + e.getMessage());
                }
            }
        } catch (WebClientResponseException e) {
            throw new IllegalArgumentException("Failed to book new doctor slot for reschedule: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Doctor Service during new slot booking for reschedule: " + e.getMessage(), e);
        }

        appointment.setAppointmentDateTime(newAppointmentTime);
        appointment.setDoctorId(newDoctorId);
        appointment.setDoctorSlotId(newDoctorSlotId);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // --- NEW: Publish AppointmentRescheduledEvent to Kafka ---
        try {
            String topicName = "appointment-rescheduled-events"; // Define this in application.properties

            AppointmentRescheduledEvent.Builder eventBuilder = AppointmentRescheduledEvent.newBuilder()
                    .setAppointmentId(updatedAppointment.getId().toString())
                    .setPatientId(updatedAppointment.getPatientId().toString())
                    .setDoctorId(updatedAppointment.getDoctorId().toString())
                    .setOldAppointmentDateTime(oldAppointmentDateTime.toString())
                    .setNewAppointmentDateTime(updatedAppointment.getAppointmentDateTime().toString())
                    .setOldDoctorId(oldDoctorId.toString())
                    .setOldDoctorSlotId(oldDoctorSlotId.toString())
                    .setNewDoctorSlotId(updatedAppointment.getDoctorSlotId().toString())
                    .setStatus(updatedAppointment.getStatus().name())
                    .setEventType("APPOINTMENT_RESCHEDULED")
                    .setTimestamp(LocalDateTime.now().toString());

            if (patient != null) {
                eventBuilder.setPatientName(patient.getName())
                        .setPatientEmail(patient.getEmail());
            }
            // Populate old doctor name if available
            if (oldDoctorDetails != null) {
                eventBuilder.setOldDoctorName(oldDoctorDetails.getFirstName() + " " + oldDoctorDetails.getLastName());
            } else {
                eventBuilder.setOldDoctorName("Unknown Doctor"); // Default if old doctor details not fetched
            }

            if (newDoctorDetails != null) {
                eventBuilder.setDoctorName(newDoctorDetails.getFirstName() + " " + newDoctorDetails.getLastName())
                        .setDoctorSpecialization(newDoctorDetails.getSpecialization())
                        .setEstimatedFeeAmount(newDoctorDetails.getConsultationFee())
                        .setCurrency("INR");
            } else {
                eventBuilder.setDoctorName("Unknown Doctor") // Default if new doctor details not fetched
                        .setDoctorSpecialization("Unknown Specialization")
                        .setEstimatedFeeAmount(0.0)
                        .setCurrency("UNKNOWN");
            }

            kafkaTemplate.send(topicName, updatedAppointment.getId().toString(), eventBuilder.build().toByteArray());
            logger.info("Published Protobuf AppointmentRescheduledEvent for appointmentId: {}", updatedAppointment.getId());

        } catch (Exception e) {
            logger.error("Failed to publish Protobuf AppointmentRescheduledEvent for appointmentId {}: {}", updatedAppointment.getId(), e.getMessage(), e);
        }
        // --- END Publish AppointmentRescheduledEvent ---

        return AppointmentMapper.toDTO(updatedAppointment);
    }


    @Transactional
    public AppointmentResponseDTO completeAppointment(UUID appointmentId, UUID requestingUserId, boolean isAdmin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!isAdmin && (requestingUserId == null || !appointment.getDoctorId().equals(requestingUserId))) {
            throw new SecurityException("You are not authorized to mark this appointment as complete. Only the associated doctor or an ADMIN can do so.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Appointment with ID '" + appointmentId + "' is already completed.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELED || appointment.getStatus() == AppointmentStatus.FAILED) {
            throw new IllegalArgumentException("Cannot complete a " + appointment.getStatus().name().toLowerCase() + " appointment.");
        }
        if (appointment.getAppointmentDateTime().isAfter(LocalDateTime.now().plusHours(1))) {
            logger.warn("Attempt to complete future appointment: {}", appointmentId);
        }

        double consultationFee;
        try {
            consultationFee = doctorServiceWebClient.get()
                    .uri("/doctors/{doctorId}/fee", appointment.getDoctorId())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ResourceNotFoundException("Doctor not found or consultation fee not configured for ID: " + appointment.getDoctorId()));
                        }
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Doctor Service error getting fee for doctor " + appointment.getDoctorId() + ": " + errorBody)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new RuntimeException("Doctor Service internal error getting fee for doctor " + appointment.getDoctorId()))
                    )
                    .bodyToMono(Double.class)
                    .block();

            if (consultationFee <= 0) {
                throw new IllegalArgumentException("Invalid consultation fee received from Doctor Service: " + consultationFee);
            }

            logger.info("Fetched consultation fee of {} for doctor ID: {}", consultationFee, appointment.getDoctorId());

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to fetch doctor's consultation fee: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Doctor Service to get fee: " + e.getMessage(), e);
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment completedAppointment = appointmentRepository.save(appointment);

        try {
            String topicName = "appointment-completed-events";

            AppointmentCompletedEvent event = AppointmentCompletedEvent.newBuilder()
                    .setAppointmentId(completedAppointment.getId().toString())
                    .setPatientId(completedAppointment.getPatientId().toString())
                    .setDoctorId(completedAppointment.getDoctorId().toString())
                    .setCompletionDateTime(LocalDateTime.now().toString())
                    .setBaseFeeAmount(consultationFee)
                    .setCurrency("INR")
                    .build();

            kafkaTemplate.send(topicName, completedAppointment.getId().toString(), event.toByteArray());
            logger.info("Published Protobuf AppointmentCompletedEvent for appointmentId: {}", completedAppointment.getId());

        } catch (Exception e) {
            logger.error("Failed to publish Protobuf AppointmentCompletedEvent for appointmentId {}: {}", appointmentId, e.getMessage(), e);
        }

        return AppointmentMapper.toDTO(completedAppointment);
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
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELED && a.getStatus() != AppointmentStatus.FAILED)
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