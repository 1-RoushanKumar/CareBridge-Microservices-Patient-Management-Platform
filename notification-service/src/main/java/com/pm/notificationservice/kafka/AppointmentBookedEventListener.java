package com.pm.notificationservice.kafka;

import appointment.events.AppointmentBookedEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AppointmentBookedEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBookedEventListener.class);
    private final NotificationService notificationService;

    public AppointmentBookedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${kafka.topics.appointment-booked:appointment-booked-events}",
            groupId = "notification-service-group") // Removed errorHandler attribute
    public void handleAppointmentBookedEvent(byte[] messageBytes) {
        try {
            AppointmentBookedEvent event = AppointmentBookedEvent.parseFrom(messageBytes);
            log.info("Received AppointmentBookedEvent for appointmentId: {}", event.getAppointmentId());

            notificationService.sendAppointmentConfirmationNotification(
                    UUID.fromString(event.getAppointmentId()),
                    UUID.fromString(event.getPatientId()),
                    event.getPatientName(),
                    event.getPatientEmail(),
                    event.getDoctorName(),
                    event.getDoctorSpecialization(),
                    LocalDateTime.parse(event.getAppointmentDateTime()),
                    event.getEstimatedFeeAmount(),
                    event.getCurrency()
            );

            log.info("Notification process triggered for appointmentId: {}", event.getAppointmentId());

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse AppointmentBookedEvent Protobuf message from Kafka: {}", e.getMessage(), e);
            // Consider DLQ or specific retry logic
        } catch (Exception e) {
            log.error("Error handling AppointmentBookedEvent for appointmentId {}: {}",
                    (messageBytes != null ? new String(messageBytes) : "N/A"), // Log raw message snippet
                    e.getMessage(), e);
            // Re-throwing might cause re-delivery if not handled by Spring Kafka's error handler
        }
    }
}