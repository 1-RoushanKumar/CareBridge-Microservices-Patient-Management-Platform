package com.pm.patientservice.kafka;

import billing.events.BillingAccountEvent;
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

    @KafkaListener(topics = "${kafka.topics.billing-account-events:billing-account-events}", groupId = "patient-service-billing-event-group")
    @Transactional
    public void handleBillingAccountEvent(byte[] messageBytes) {
        try {
            BillingAccountEvent event = BillingAccountEvent.parseFrom(messageBytes);
            log.info("Patient-Service: Received BillingAccountEvent: {} for patientId {}", event.getEventType(), event.getPatientId());

            UUID patientUuid = UUID.fromString(event.getPatientId());
            Patient patient = patientRepository.findById(patientUuid)
                    .orElse(null);

            if (patient == null) {
                log.warn("Patient-Service: Patient with ID {} not found. Cannot update billing status for event {}.", event.getPatientId(), event.getEventType());
                return;
            }

            if ("BILLING_ACCOUNT_CREATED".equals(event.getEventType()) || "BILLING_ACCOUNT_UPDATED".equals(event.getEventType())) {
                patient.setBillingAccountStatus(event.getStatus());
                patientRepository.save(patient);
                log.info("Patient-Service: Updated patient {} billing status to: {}", patient.getId(), patient.getBillingAccountStatus());
            } else {
                log.info("Patient-Service: Unhandled BillingAccountEvent type: {} for patientId {}", event.getEventType(), event.getPatientId());
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Patient-Service: Failed to parse BillingAccountEvent Protobuf message from Kafka: {}", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Patient-Service: Invalid patientId UUID in BillingAccountEvent: {}. Message bytes: {}", e.getMessage(), new String(messageBytes), e);
        } catch (Exception e) {
            log.error("Patient-Service: Unexpected error handling BillingAccountEvent for message (bytes): {}. Error: {}",
                    new String(messageBytes), e.getMessage(), e);
        }
    }
}