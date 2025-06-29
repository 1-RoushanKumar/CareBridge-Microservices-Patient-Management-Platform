package com.roushan.appointmentservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.roushan.appointmentservice.model.PatientDetails;
import com.roushan.appointmentservice.repository.PatientDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import patient.events.PatientEvent; // This is the generated Protobuf class

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

    @KafkaListener(topics = "${kafka.topics.patient-events:patient}", groupId = "appointment-service-patient-event-group") // Changed topic name to be more generic for events
    @Transactional
    public void handlePatientEvent(byte[] messageBytes) {
        PatientEvent patientEvent;
        try {
            patientEvent = PatientEvent.parseFrom(messageBytes);
        } catch (InvalidProtocolBufferException e) {
            log.error("Appointment-Service: Failed to parse PatientEvent Protobuf message from Kafka. Message bytes: {}. Error: {}",
                    bytesToHexString(messageBytes), e.getMessage(), e);
            return;
        }

        log.info("Appointment-Service: Received PatientEvent: {} for patientId: {}",
                patientEvent.getEventType(), patientEvent.getPatientId());

        UUID patientId;
        try {
            patientId = UUID.fromString(patientEvent.getPatientId());
        } catch (IllegalArgumentException e) {
            log.error("Appointment-Service: Invalid patientId UUID format in PatientEvent: '{}'. Event details: {}. Error: {}",
                    patientEvent.getPatientId(), patientEvent.toString(), e.getMessage(), e);
            return;
        }

        try {
            Optional<PatientDetails> existingPatientDetails = patientDetailsRepository.findById(patientId);
            PatientDetails patientDetails;

            switch (patientEvent.getEventType()) {
                case "PATIENT_CREATED":
                    if (existingPatientDetails.isPresent()) {
                        log.warn("Appointment-Service: PATIENT_CREATED event received for existing PatientDetails ID: {}. Updating existing entry.", patientId);
                        patientDetails = existingPatientDetails.get();
                    } else {
                        log.info("Appointment-Service: Creating new PatientDetails entry for ID: {}", patientId);
                        patientDetails = new PatientDetails();
                        patientDetails.setId(patientId);
                    }
                    patientDetails.setName(patientEvent.getName());
                    patientDetails.setEmail(patientEvent.getEmail());
                    patientDetails.setStatus("ACTIVE"); // Assuming created patients are active by default
                    patientDetails.setLastUpdated(LocalDateTime.now());
                    patientDetailsRepository.save(patientDetails);
                    log.info("Appointment-Service: PatientDetails created/updated for patientId: {}", patientId);
                    break;

                case "PATIENT_UPDATED":
                    if (existingPatientDetails.isPresent()) {
                        patientDetails = existingPatientDetails.get();
                        patientDetails.setName(patientEvent.getName());
                        patientDetails.setEmail(patientEvent.getEmail());
                        patientDetails.setLastUpdated(LocalDateTime.now());
                        patientDetailsRepository.save(patientDetails);
                        log.info("Appointment-Service: PatientDetails updated for patientId: {}", patientId);
                    } else {
                        log.warn("Appointment-Service: PATIENT_UPDATED event received for non-existent PatientDetails ID: {}. Cannot update.", patientId);
                    }
                    break;

                case "PATIENT_DELETED":
                    if (existingPatientDetails.isPresent()) {
                        patientDetails = existingPatientDetails.get();
                        patientDetails.setStatus("DELETED");
                        patientDetails.setLastUpdated(LocalDateTime.now());
                        patientDetailsRepository.save(patientDetails);
                        log.info("Appointment-Service: PatientDetails marked as DELETED for patientId: {}", patientId);
                    } else {
                        log.warn("Appointment-Service: PATIENT_DELETED event received for non-existent PatientDetails ID: {}. Nothing to delete.", patientId);
                    }
                    break;

                default:
                    log.warn("Appointment-Service: Received unknown PatientEvent type: '{}' for patientId: {}", patientEvent.getEventType(), patientId);
            }
        } catch (Exception e) {
            log.error("Appointment-Service: Error processing PatientEvent for patientId: {}. Event details: {}. Error: {}",
                    patientId, patientEvent.toString(), e.getMessage(), e);
            throw new RuntimeException("Error processing patient event.", e);
        }
    }

    // Helper to convert byte array to hex string for better logging of raw messages
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}