// src/main/java/com/pm/billingservice/kafka/AppointmentCompletedEventListener.java
package com.pm.billingservice.kafka;

import appointment.events.AppointmentCompletedEvent; // Import the generated Protobuf class
import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.billingservice.service.BillService; // Import the new BillService
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AppointmentCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCompletedEventListener.class);
    private final BillService billService; // Inject the new BillService

    public AppointmentCompletedEventListener(BillService billService) {
        this.billService = billService;
    }

    // Configure Kafka listener for the topic that Appointment Service publishes to
    @KafkaListener(topics = "${kafka.topics.appointment-completed:appointment-completed-events}",
            groupId = "billing-service-group-appointment-completion") // Use a distinct group ID
    public void handleAppointmentCompletedEvent(byte[] messageBytes) {
        try {
            // Deserialize the byte array back into an AppointmentCompletedEvent Protobuf object
            AppointmentCompletedEvent event = AppointmentCompletedEvent.parseFrom(messageBytes);

            log.info("Received AppointmentCompletedEvent for appointmentId: {}", event.getAppointmentId());

            billService.generateBillForAppointment(
                    UUID.fromString(event.getAppointmentId()),
                    UUID.fromString(event.getPatientId()),
                    UUID.fromString(event.getDoctorId()),
                    LocalDateTime.parse(event.getCompletionDateTime()), // Parse the timestamp string
                    event.getBaseFeeAmount(),
                    event.getCurrency()
            );

            log.info("Bill generation process triggered for appointmentId: {}", event.getAppointmentId());

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse AppointmentCompletedEvent Protobuf message from Kafka: {}", e.getMessage(), e);
            // Implement Dead Letter Queue (DLQ) or robust error handling here
        } catch (Exception e) {
            log.error("Error handling AppointmentCompletedEvent for messageBytes (first few) '{}': {}",
                    new String(messageBytes, 0, Math.min(messageBytes.length, 100)), // Log a snippet
                    e.getMessage(), e);
            // Consider specific retry mechanisms or DLQ for transient errors.
        }
    }
}