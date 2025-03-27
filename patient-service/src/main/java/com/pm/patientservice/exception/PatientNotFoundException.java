package com.pm.patientservice.exception;

// Custom exception to indicate that a patient was not found
public class PatientNotFoundException extends RuntimeException {

    // Constructor that takes an error message and passes it to the RuntimeException superclass
    public PatientNotFoundException(String message) {
        super(message);
    }
}