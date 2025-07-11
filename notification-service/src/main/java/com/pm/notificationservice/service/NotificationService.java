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
    private final JavaMailSender javaMailSender;

    @Value("${notification.email.from:no-reply@patient-management.com}")
    private String senderEmail;

    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendAppointmentConfirmationNotification(
            UUID appointmentId, UUID patientId, String patientName, String patientEmail,
            String doctorName, String doctorSpecialization, LocalDateTime appointmentDateTime,
            double estimatedFeeAmount, String currency) {

        String subject = "Appointment Confirmation - ID " + appointmentId.toString();

        String notificationMessage = String.format(
                "Dear %s,\n\n" +
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

        sendEmail(patientEmail, subject, notificationMessage);
    }

    // NEW METHOD FOR CANCELED APPOINTMENTS
    public void sendAppointmentCancellationNotification(
            UUID appointmentId, UUID patientId, String patientName, String patientEmail,
            String doctorName, String doctorSpecialization, LocalDateTime originalAppointmentDateTime,
            double estimatedFeeAmount, String currency, String cancellationReason) {

        String subject = "Appointment Canceled - ID " + appointmentId.toString();

        String notificationMessage = String.format(
                "Dear %s,\n\n" +
                "Your appointment (ID: %s) originally scheduled with Dr. %s (%s) on %s has been **canceled**.\n\n" +
                "Reason for cancellation: %s\n\n" +
                "If you wish to re-book, please visit our portal or contact support.\n\n" +
                "Regards,\n" +
                "Patient Management Team",
                patientName, appointmentId.toString(), doctorName, doctorSpecialization,
                originalAppointmentDateTime.toString(), cancellationReason
        );

        log.info("--- Preparing Appointment Cancellation Notification ---");
        log.info("To: {} ({})", patientName, patientEmail);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", notificationMessage);
        log.info("-----------------------------------------------------");

        sendEmail(patientEmail, subject, notificationMessage);
    }

    // NEW METHOD FOR RESCHEDULED APPOINTMENTS
    public void sendAppointmentRescheduleNotification(
            UUID appointmentId, UUID patientId, String patientName, String patientEmail,
            String oldAppointmentDateTime, String newAppointmentDateTime,
            String oldDoctorName, String newDoctorName, String newDoctorSpecialization,
            double estimatedFeeAmount, String currency) {

        String subject = "Appointment Rescheduled - ID " + appointmentId.toString();

        String doctorChangeMessage = "";
        if (!oldDoctorName.equals(newDoctorName)) {
            doctorChangeMessage = String.format("- Doctor changed from Dr. %s to Dr. %s (%s)\n",
                    oldDoctorName, newDoctorName, newDoctorSpecialization);
        }

        String notificationMessage = String.format(
                "Dear %s,\n\n" +
                "Your appointment (ID: %s) has been **rescheduled**!\n\n" +
                "**Original Details:**\n" +
                "- Time: %s\n" +
                "%s" + // Doctor change message, if any
                "**New Details:**\n" +
                "- Doctor: Dr. %s (%s)\n" +
                "- Time: %s\n" +
                "- Estimated Fee: %.2f %s\n\n" +
                "Please make a note of the new details. We look forward to seeing you.\n\n" +
                "Regards,\n" +
                "Patient Management Team",
                patientName, appointmentId.toString(),
                oldAppointmentDateTime,
                doctorChangeMessage,
                newDoctorName, newDoctorSpecialization, newAppointmentDateTime,
                estimatedFeeAmount, currency
        );

        log.info("--- Preparing Appointment Reschedule Notification ---");
        log.info("To: {} ({})", patientName, patientEmail);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", notificationMessage);
        log.info("-----------------------------------------------------");

        sendEmail(patientEmail, subject, notificationMessage);
    }

    // Helper method to consolidate email sending logic
    private void sendEmail(String recipientEmail, String subject, String content) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(content, false); // Assuming plain text for now

            javaMailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", recipientEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} with subject {}: {}", recipientEmail, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while sending email to {} with subject {}: {}", recipientEmail, subject, e.getMessage(), e);
        }
    }
}