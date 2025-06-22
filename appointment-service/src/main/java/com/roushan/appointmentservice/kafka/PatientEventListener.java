package com.roushan.appointmentservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.roushan.appointmentservice.model.PatientDetails;
import com.roushan.appointmentservice.repository.PatientDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import patient.events.PatientEvent;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class PatientEventListener {

    private static final Logger log = LoggerFactory.getLogger(PatientEventListener.class);
    private final PatientDetailsRepository patientDetailsRepository;

    public PatientEventListener(PatientDetailsRepository patientDetailsRepository) {
        this.patientDetailsRepository = patientDetailsRepository;
    }

    // Configure Kafka listener. Ensure the 'topics' matches what patient-service sends.
    @KafkaListener(topics = "${kafka.topics.patient-created:patient}", groupId = "appointment-service-patient-event-group")
    @Transactional
    public void handlePatientEvent(byte[] messageBytes) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(messageBytes);

            log.info("Appointment-Service: Received PatientEvent: {} for patientId: {} with status: {}",
                    patientEvent.getEventType(), patientEvent.getPatientId(), patientEvent.getStatus()); // Updated log

            UUID patientId = UUID.fromString(patientEvent.getPatientId());
            Optional<PatientDetails> existingPatientDetails = patientDetailsRepository.findById(patientId);

            PatientDetails patientDetails;

            // Handle PATIENT_CREATED and PATIENT_UPDATED similarly by creating/updating
            if ("PATIENT_CREATED".equals(patientEvent.getEventType()) || "PATIENT_UPDATED".equals(patientEvent.getEventType())) {
                if (existingPatientDetails.isPresent()) {
                    log.info("Appointment-Service: Updating existing PatientDetails entry for ID: {}", patientId);
                    patientDetails = existingPatientDetails.get();
                } else {
                    if ("PATIENT_UPDATED".equals(patientEvent.getEventType())) {
                        log.warn("Appointment-Service: PATIENT_UPDATED event received for non-existent PatientDetails ID: {}. Creating new entry.", patientId);
                    } else {
                        log.info("Appointment-Service: Creating new PatientDetails entry for ID: {}", patientId);
                    }
                    patientDetails = new PatientDetails();
                    patientDetails.setId(patientId);
                }
                patientDetails.setName(patientEvent.getName());
                patientDetails.setEmail(patientEvent.getEmail());
                patientDetails.setStatus(patientEvent.getStatus()); // <-- Use status from the event!
                patientDetails.setLastUpdated(LocalDateTime.now());
                patientDetailsRepository.save(patientDetails);
                log.info("Appointment-Service: PatientDetails created/updated for patientId: {} with status: {}", patientId, patientEvent.getStatus());

            } else if ("PATIENT_DELETED".equals(patientEvent.getEventType())) {
                if (existingPatientDetails.isPresent()) {
                    patientDetails = existingPatientDetails.get();
                    patientDetails.setStatus("DELETED"); // Explicitly set to DELETED for logical deletion
                    patientDetails.setLastUpdated(LocalDateTime.now());
                    patientDetailsRepository.save(patientDetails); // Or consider patientDetailsRepository.delete(patientDetails);
                    log.info("Appointment-Service: PatientDetails marked as DELETED for patientId: {}", patientId);
                } else {
                    log.warn("Appointment-Service: PATIENT_DELETED event received for non-existent PatientDetails ID: {}. Ignoring.", patientId);
                }
            } else {
                log.warn("Appointment-Service: Received unknown PatientEvent type: {} for patientId: {}", patientEvent.getEventType(), patientEvent.getPatientId());
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Appointment-Service: Failed to parse PatientEvent Protobuf message from Kafka: {}", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Appointment-Service: Invalid patientId UUID in PatientEvent: {}. Message bytes: {}", e.getMessage(), new String(messageBytes), e);
        } catch (Exception e) {
            log.error("Appointment-Service: Error handling PatientEvent for message (bytes): {}. Error: {}",
                    new String(messageBytes), e.getMessage(), e);
        }
    }
}