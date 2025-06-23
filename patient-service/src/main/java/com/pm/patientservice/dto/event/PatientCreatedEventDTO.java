package com.pm.patientservice.dto.event;

import java.io.Serializable;

public record PatientCreatedEventDTO(
        String patientId,
        String name,
        String email,
        String registeredDate
) implements Serializable {
}