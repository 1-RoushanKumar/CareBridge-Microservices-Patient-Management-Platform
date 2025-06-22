package com.pm.patientservice.kafka;

import billing.events.BillingAccountEvent; // Import the generated Protobuf class
import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class BillingAccountEventListener {

    private static final Logger log = LoggerFactory.getLogger(BillingAccountEventListener.class);
    private final PatientRepository patientRepository;

    public BillingAccountEventListener(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    // This listener will consume messages from the 'billing-account-events' topic
    // Ensure the groupId is unique for this consumer instance/service
    @KafkaListener(topics = "${kafka.topics.billing-account-events:billing-account-events}", groupId = "patient-service-billing-event-group")
    @Transactional // Ensures the database operation is atomic
    public void handleBillingAccountEvent(byte[] messageBytes) {
        try {
            BillingAccountEvent event = BillingAccountEvent.parseFrom(messageBytes);
            log.info("Patient-Service: Received BillingAccountEvent: {} for patientId {}", event.getEventType(), event.getPatientId());

            // Find the patient by the patientId from the event
            UUID patientUuid = UUID.fromString(event.getPatientId());
            Patient patient = patientRepository.findById(patientUuid)
                    .orElse(null); // Return null if not found, or throw specific exception

            if (patient == null) {
                log.warn("Patient-Service: Patient with ID {} not found. Cannot update billing status for event {}.", event.getPatientId(), event.getEventType());
                // Consider sending this to a Dead Letter Queue (DLQ) if this is a critical data inconsistency.
                return;
            }

            // Update the patient's billing status based on the event's status and type
            if ("BILLING_ACCOUNT_CREATED".equals(event.getEventType()) || "BILLING_ACCOUNT_UPDATED".equals(event.getEventType())) {
                patient.setBillingAccountStatus(event.getStatus()); // Set status from the event (e.g., "ACTIVE")
                patientRepository.save(patient);
                log.info("Patient-Service: Updated patient {} billing status to: {}", patient.getId(), patient.getBillingAccountStatus());
            }
            // You can add more specific logic here for other event types
            // else if ("BILLING_ACCOUNT_DELETED".equals(event.getEventType())) { ... }

        } catch (InvalidProtocolBufferException e) {
            log.error("Patient-Service: Failed to parse BillingAccountEvent Protobuf message from Kafka: {}", e.getMessage(), e);
            // This indicates a deserialization problem; likely a malformed message or schema mismatch.
        } catch (IllegalArgumentException e) { // Catches errors from UUID.fromString
            log.error("Patient-Service: Invalid patientId UUID in BillingAccountEvent: {}. Message bytes: {}", e.getMessage(), new String(messageBytes), e);
        } catch (Exception e) {
            log.error("Patient-Service: Unexpected error handling BillingAccountEvent for message (bytes): {}. Error: {}",
                    new String(messageBytes), e.getMessage(), e);
            // General catch-all for any other runtime exceptions during processing.
        }
    }
}