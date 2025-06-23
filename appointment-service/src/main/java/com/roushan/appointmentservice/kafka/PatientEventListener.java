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

    // Configure Kafka listener. Ensure the 'topics' matches what patient-service sends.
    @KafkaListener(topics = "${kafka.topics.patient-created:patient}", groupId = "appointment-service-patient-event-group")
    @Transactional // Ensure atomicity for database operations
    public void handlePatientEvent(byte[] messageBytes) {
        try {
            // Deserialize the byte array back into a PatientEvent Protobuf object
            PatientEvent patientEvent = PatientEvent.parseFrom(messageBytes);

            log.info("Appointment-Service: Received PatientEvent: {} for patientId: {}",
                    patientEvent.getEventType(), patientEvent.getPatientId());

            UUID patientId = UUID.fromString(patientEvent.getPatientId());
            Optional<PatientDetails> existingPatientDetails = patientDetailsRepository.findById(patientId);

            PatientDetails patientDetails;

            if ("PATIENT_CREATED".equals(patientEvent.getEventType())) {
                if (existingPatientDetails.isPresent()) {
                    log.warn("Appointment-Service: PatientDetails for ID {} already exists. Updating existing entry.", patientId);
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

            } else if ("PATIENT_UPDATED".equals(patientEvent.getEventType())) {
                if (existingPatientDetails.isPresent()) {
                    patientDetails = existingPatientDetails.get();
                    // Update relevant fields. PatientEvent might need more fields for updates if status changes.
                    // For simplicity, let's just update name, email, and assume status remains active or is derived.
                    patientDetails.setName(patientEvent.getName());
                    patientDetails.setEmail(patientEvent.getEmail());
                    // If patient-service sends 'status' in PatientEvent for updates, use it:
                    // patientDetails.setStatus(patientEvent.getStatus());
                    patientDetails.setLastUpdated(LocalDateTime.now());
                    patientDetailsRepository.save(patientDetails);
                    log.info("Appointment-Service: PatientDetails updated for patientId: {}", patientId);
                } else {
                    log.warn("Appointment-Service: PATIENT_UPDATED event received for non-existent PatientDetails ID: {}. Ignoring.", patientId);
                }
            } else if ("PATIENT_DELETED".equals(patientEvent.getEventType())) {
                if (existingPatientDetails.isPresent()) {
                    patientDetails = existingPatientDetails.get();
                    patientDetails.setStatus("DELETED"); // Mark as deleted, or simply delete the record
                    patientDetails.setLastUpdated(LocalDateTime.now());
                    patientDetailsRepository.save(patientDetails); // Or patientDetailsRepository.delete(patientDetails);
                    log.info("Appointment-Service: PatientDetails marked as DELETED for patientId: {}", patientId);
                } else {
                    log.warn("Appointment-Service: PATIENT_DELETED event received for non-existent PatientDetails ID: {}. Ignoring.", patientId);
                }
            } else {
                log.warn("Appointment-Service: Received unknown PatientEvent type: {} for patientId: {}", patientEvent.getEventType(), patientEvent.getPatientId());
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Appointment-Service: Failed to parse PatientEvent Protobuf message from Kafka: {}", e.getMessage(), e);
            // For production, consider moving to a Dead Letter Queue (DLQ)
        } catch (IllegalArgumentException e) { // Catches errors from UUID.fromString
            log.error("Appointment-Service: Invalid patientId UUID in PatientEvent: {}. Message bytes: {}", e.getMessage(), new String(messageBytes), e);
        } catch (Exception e) {
            log.error("Appointment-Service: Error handling PatientEvent for message (bytes): {}. Error: {}",
                    new String(messageBytes), e.getMessage(), e);
        }
    }
}