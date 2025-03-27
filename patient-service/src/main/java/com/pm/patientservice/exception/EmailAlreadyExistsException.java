package com.pm.patientservice.exception;

// Custom exception to indicate that an email address is already registered
public class EmailAlreadyExistsException extends RuntimeException {

    // Constructor that takes an error message and passes it to the RuntimeException superclass
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}