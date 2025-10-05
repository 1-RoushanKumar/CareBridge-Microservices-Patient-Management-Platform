package com.pm.billingservice.kafka;

import billing.events.BillingAccountEvent; // Import the new Protobuf DTO
import billing.events.PaymentReceivedEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import notification.request.events.PaymentNotificationRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BillingEventProducer {

    private static final Logger log = LoggerFactory.getLogger(BillingEventProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate; // KafkaTemplate should send byte[] for Protobuf

    @Value("${kafka.topics.billing-account-events:billing-account-events}") // Define new topic name
    private String billingAccountTopic;

    @Value("${kafka.topics.payment-events:payment-events}") // NEW TOPIC for payment events
    private String paymentEventsTopic;

    // NEW: Topic for sending notification requests
    @Value("${kafka.topics.payment-notification-requests:payment-notification-requests}")
    private String paymentNotificationRequestTopic;

    public BillingEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBillingAccountEvent(BillingAccountEvent event) {
        try {
            // Use patientId or accountId as key for partitioning
            kafkaTemplate.send(billingAccountTopic, event.getPatientId(), event.toByteArray());
            log.info("Produced Kafka message to topic '{}' for billing account {}: {}",
                    billingAccountTopic, event.getAccountId(), event.getEventType());
        } catch (Exception e) {
            log.error("Error sending BillingAccountEvent for account {}: {}", event.getAccountId(), e.getMessage(), e);
            // Consider robust error handling (e.g., retry, DLQ)
        }
    }

    // NEW METHOD: Send PaymentReceivedEvent
    public void sendPaymentReceivedEvent(PaymentReceivedEvent event) {
        try {
            // Use billId or patientId as key for partitioning
            kafkaTemplate.send(paymentEventsTopic, event.getBillId(), event.toByteArray());
            log.info("Produced Kafka message to topic '{}' for payment transaction {}: {}",
                    paymentEventsTopic, event.getTransactionId(), event.getEventType());
        } catch (Exception e) {
            log.error("Error sending PaymentReceivedEvent for transaction {}: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }

    // NEW METHOD: Send a specific event for notifications
    public void sendPaymentNotificationRequest(PaymentNotificationRequestEvent event) {
        try {
            kafkaTemplate.send(paymentNotificationRequestTopic, event.getBillId(), event.toByteArray());
            log.info("Produced payment notification request to topic '{}' for transaction {}",
                    paymentNotificationRequestTopic, event.getTransactionId());
        } catch (Exception e) {
            log.error("Error sending PaymentNotificationRequestEvent for transaction {}: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }
}