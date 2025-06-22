package com.pm.billingservice.kafka;

import billing.events.BillingAccountEvent; // Import the new Protobuf DTO
import com.google.protobuf.InvalidProtocolBufferException;
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
}