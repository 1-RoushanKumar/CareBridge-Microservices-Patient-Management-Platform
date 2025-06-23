package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        try {
            PatientEvent event = PatientEvent.newBuilder()
                    .setPatientId(patient.getId().toString())
                    .setName(patient.getName())
                    .setEmail(patient.getEmail())
                    .setEventType("PATIENT_CREATED")
                    .setRegisteredDate(patient.getRegisteredDate().toString()) // Added registeredDate to event
                    .build();

            kafkaTemplate.send("patient", event.toByteArray());
            log.info("Patient-Service: Sent PatientCreated event for patientId {}", patient.getId());
        } catch (Exception e) {
            log.error("Patient-Service: Error sending PatientCreated event for patient with ID {}: {}", patient.getId(), e.getMessage(), e);
        }
    }
}