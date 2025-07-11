package com.pm.notificationservice.kafka;

import appointment.events.AppointmentBookedEvent;
import appointment.events.AppointmentCanceledEvent;
import appointment.events.AppointmentRescheduledEvent;
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

    // NEW LISTENER FOR RESCHEDULED EVENTS
    @KafkaListener(topics = "${kafka.topics.appointment-rescheduled:appointment-rescheduled-events}",
            groupId = "notification-service-group")
    public void handleAppointmentRescheduledEvent(byte[] messageBytes) {
        try {
            AppointmentRescheduledEvent event = AppointmentRescheduledEvent.parseFrom(messageBytes);
            log.info("Received AppointmentRescheduledEvent for appointmentId: {}", event.getAppointmentId());

            notificationService.sendAppointmentRescheduleNotification(
                    UUID.fromString(event.getAppointmentId()),
                    UUID.fromString(event.getPatientId()),
                    event.getPatientName(),
                    event.getPatientEmail(),
                    event.getOldAppointmentDateTime(), // Pass old and new details
                    event.getNewAppointmentDateTime(),
                    event.getOldDoctorName(), // Assuming you have old doctor name or fetching it if changed
                    event.getDoctorName(), // New doctor name
                    event.getDoctorSpecialization(), // New specialization
                    event.getEstimatedFeeAmount(),
                    event.getCurrency()
            );

            log.info("Notification process triggered for rescheduled appointmentId: {}", event.getAppointmentId());

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse AppointmentRescheduledEvent Protobuf message from Kafka: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error handling AppointmentRescheduledEvent for appointmentId {}: {}",
                    (messageBytes != null ? new String(messageBytes) : "N/A"),
                    e.getMessage(), e);
        }
    }

    // NEW LISTENER FOR CANCELED EVENTS
    @KafkaListener(topics = "${kafka.topics.appointment-canceled:appointment-canceled-events}",
            groupId = "notification-service-group")
    public void handleAppointmentCanceledEvent(byte[] messageBytes) {
        try {
            AppointmentCanceledEvent event = AppointmentCanceledEvent.parseFrom(messageBytes);
            log.info("Received AppointmentCanceledEvent for appointmentId: {}", event.getAppointmentId());

            notificationService.sendAppointmentCancellationNotification(
                    UUID.fromString(event.getAppointmentId()),
                    UUID.fromString(event.getPatientId()),
                    event.getPatientName(),
                    event.getPatientEmail(),
                    event.getDoctorName(),
                    event.getDoctorSpecialization(),
                    LocalDateTime.parse(event.getAppointmentDateTime()), // Original appointment time
                    event.getEstimatedFeeAmount(),
                    event.getCurrency(),
                    event.getCancellationReason() // Pass the cancellation reason
            );

            log.info("Notification process triggered for canceled appointmentId: {}", event.getAppointmentId());

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse AppointmentCanceledEvent Protobuf message from Kafka: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error handling AppointmentCanceledEvent for appointmentId {}: {}",
                    (messageBytes != null ? new String(messageBytes) : "N/A"),
                    e.getMessage(), e);
        }
    }
}