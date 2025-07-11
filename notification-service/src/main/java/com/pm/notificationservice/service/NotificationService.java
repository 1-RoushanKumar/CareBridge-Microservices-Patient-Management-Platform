package com.pm.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender; // NEW: Inject JavaMailSender

    @Value("${notification.email.from:no-reply@patient-management.com}") // NEW: Get 'from' address from properties
    private String senderEmail;

    // Modified Constructor to inject JavaMailSender
    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendAppointmentConfirmationNotification(
            UUID appointmentId, UUID patientId, String patientName, String patientEmail,
            String doctorName, String doctorSpecialization, LocalDateTime appointmentDateTime,
            double estimatedFeeAmount, String currency) {

        String subject = "Appointment Confirmation - ID " + appointmentId.toString();

        String notificationMessage = String.format(
                "Dear %s,\n\n" + // Added an extra newline for better email formatting
                "Your appointment (ID: %s) has been successfully booked!\n\n" +
                "**Appointment Details:**\n" +
                "- Doctor: Dr. %s (%s)\n" +
                "- Time: %s\n" +
                "- Estimated Fee: %.2f %s\n\n" +
                "We look forward to seeing you. Please arrive 15 minutes early.\n\n" +
                "Regards,\n" +
                "Patient Management Team",
                patientName, appointmentId.toString(), doctorName, doctorSpecialization,
                appointmentDateTime.toString(), estimatedFeeAmount, currency
        );

        log.info("--- Preparing Appointment Confirmation Notification ---");
        log.info("To: {} ({})", patientName, patientEmail);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", notificationMessage);
        log.info("-----------------------------------------------------");

        // NEW: Actual email sending logic
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for multipart message (e.g., HTML, attachments)

            helper.setFrom(senderEmail);
            helper.setTo(patientEmail);
            helper.setSubject(subject);
            // Use 'true' for HTML content if you want to send rich text.
            // For now, it's plain text, but if you format with HTML tags, set true.
            helper.setText(notificationMessage, false); // false for plain text

            javaMailSender.send(message);
            log.info("Email sent successfully to {} for appointment ID {}", patientEmail, appointmentId);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} for appointment ID {}: {}", patientEmail, appointmentId, e.getMessage(), e);
            // Handle specific email sending failures here (e.g., retry, dead-letter queue for notifications)
        } catch (Exception e) {
            log.error("An unexpected error occurred while sending email for appointment ID {}: {}", appointmentId, e.getMessage(), e);
        }
    }
    // You might add other notification types here (e.g., cancellation, payment reminder)
}