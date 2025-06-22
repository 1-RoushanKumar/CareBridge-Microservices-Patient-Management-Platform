package com.pm.patientservice.dto.event;

import java.io.Serializable; // Good practice for Kafka serialization

// Using Java record for conciseness (requires Java 16+)
// If using older Java, use a regular class with constructor, getters, setters.
public record PatientCreatedEventDTO(
        String patientId,
        String name,
        String email,
        String registeredDate // Include relevant info from the patient model
) implements Serializable {
    // You can add a no-arg constructor if your Kafka consumer deserializer requires it
    // public PatientCreatedEventDTO() { this(null, null, null, null); }
}
