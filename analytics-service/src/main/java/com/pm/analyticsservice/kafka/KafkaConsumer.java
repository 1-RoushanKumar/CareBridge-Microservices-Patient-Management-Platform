package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    //Using this annotation we are connecting analytics-service to the kafka topic
    //The groupId is used to identify the consumer group that this consumer belongs to
    @KafkaListener(topics = "patient", groupId = "analytics-service")
    //the kafkaProducer is sending the byte array then consumer will also receive the byte array
    public void consumeEvent(byte[] event) {
        try {
            //here we are creating a PatientEvent object from the byte array that we received from the kafka event.
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // ...performing any business logic related to the analytics service
            log.info("Received Patient Event: [PatientID: {}, PatientName: {} , PatientEmail = {} ]",
                    patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {} ", e.getMessage());
        }
    }
}
