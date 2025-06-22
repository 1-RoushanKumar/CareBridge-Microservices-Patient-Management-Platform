package com.pm.billingservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.billingservice.service.BillingAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import patient.events.PatientEvent; // This is the generated Protobuf class

@Component
public class PatientEventListener {

    private static final Logger log = LoggerFactory.getLogger(PatientEventListener.class);
    private final BillingAccountService billingAccountService;

    public PatientEventListener(BillingAccountService billingAccountService) {
        this.billingAccountService = billingAccountService;
    }

    // Configure Kafka listener. Ensure the 'topics' matches what patient-service sends.
    @KafkaListener(topics = "${kafka.topics.patient-created:patient}", groupId = "billing-service-group")
    public void handlePatientCreatedEvent(byte[] messageBytes) {
        try {
            // Deserialize the byte array back into a PatientEvent Protobuf object
            PatientEvent patientEvent = PatientEvent.parseFrom(messageBytes);

            log.info("Received PatientEvent: {}", patientEvent.getEventType());

            if ("PATIENT_CREATED".equals(patientEvent.getEventType())) {
                log.info("Processing PATIENT_CREATED event for patientId: {}", patientEvent.getPatientId());
                billingAccountService.createBillingAccount(
                        patientEvent.getPatientId(),
                        patientEvent.getName(),
                        patientEvent.getEmail()
                );
                log.info("Billing account creation process triggered for patientId: {}", patientEvent.getPatientId());
            } else {
                log.warn("Received unknown event type: {} for patientId: {}", patientEvent.getEventType(), patientEvent.getPatientId());
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse PatientEvent Protobuf message from Kafka: {}", e.getMessage(), e);
            // Important: For production, consider moving this to a Dead Letter Queue (DLQ)
            // or implementing a robust retry mechanism if parsing consistently fails.
        } catch (Exception e) {
            log.error("Error handling PatientEvent for patientId {}: {}",
                    (messageBytes != null ? new String(messageBytes) : "N/A"), // Try to log raw message
                    e.getMessage(), e);
            // Re-throwing the exception might cause Kafka to re-deliver the message,
            // potentially leading to an infinite loop if the error is persistent.
            // Use error handlers or specific retry mechanisms configured with Spring Kafka.
        }
    }
}
