package com.pm.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendAppointmentConfirmationNotification(
            UUID appointmentId, UUID patientId, String patientName, String patientEmail,
            String doctorName, String doctorSpecialization, LocalDateTime appointmentDateTime,
            double estimatedFeeAmount, String currency) {

        String notificationMessage = String.format(
                "Dear %s,\n" +
                "Your appointment (ID: %s) has been successfully booked!\n" +
                "Doctor: Dr. %s (%s)\n" +
                "Time: %s\n" +
                "Estimated Fee: %.2f %s\n" +
                "We look forward to seeing you. Please arrive 15 minutes early.\n" +
                "Regards,\n" +
                "Patient Management Team",
                patientName, appointmentId.toString(), doctorName, doctorSpecialization,
                appointmentDateTime.toString(), estimatedFeeAmount, currency
        );

        log.info("--- Sending Appointment Confirmation Notification ---");
        log.info("To: {} ({})", patientName, patientEmail);
        log.info("Subject: Appointment Confirmation - ID {}", appointmentId);
        log.info("Body:\n{}", notificationMessage);
        log.info("-----------------------------------------------------");

        // In a real application, you would integrate with:
        // - Email service (e.g., SendGrid, Mailgun, AWS SES)
        // - SMS service (e.g., Twilio, AWS SNS)
        // - Push notification service (e.g., Firebase Cloud Messaging)
        // Example: emailService.sendEmail(patientEmail, "Appointment Confirmation", notificationMessage);
    }

    // You might add other notification types here (e.g., cancellation, payment reminder)
}
