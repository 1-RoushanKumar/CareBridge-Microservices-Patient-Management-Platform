package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

/**
 * KafkaProducer is responsible for publishing patient-related events to a Kafka topic.
 * When a patient is created, this class sends a serialized PatientEvent message to Kafka.
 */
@Service // Marks this class as a Spring service component.
public class KafkaProducer {

    // Logger instance for logging messages related to Kafka event publishing.
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    // KafkaTemplate is used to send messages to a Kafka topic.
    // The key is a String (topic name), and the value is a byte array (serialized event).
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    /**
     * Constructor to initialize KafkaProducer with KafkaTemplate.
     * @param kafkaTemplate KafkaTemplate for sending messages to Kafka.
     */
    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a PatientEvent message to the Kafka "patient" topic.
     * This method is triggered when a new patient is created.
     *
     * @param patient The Patient object containing patient details.
     */
    public void sendEvent(Patient patient) {
        // Build a PatientEvent protobuf message with patient details.
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString()) // Convert patient ID to string.
                .setName(patient.getName()) // Set patient name.
                .setEmail(patient.getEmail()) // Set patient email.
                .setEventType("PATIENT_CREATED") // setEventType is used to describe a particular event inside a topic
                .build();

        try {
            // Send the serialized event message to the "patient" Kafka topic.
            kafkaTemplate.send("patient", event.toByteArray());
        } catch (Exception e) {
            // Log an error message if the event fails to send.
            log.error("Error sending PatientCreated event: {}", event);
        }
    }
}
