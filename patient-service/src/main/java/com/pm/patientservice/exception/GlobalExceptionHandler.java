package com.pm.patientservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Marks this class as a global exception handler for the entire application.
// It ensures that the methods inside this class handle exceptions globally across all controllers.
@ControllerAdvice
public class GlobalExceptionHandler {

    // This annotation tells Spring that this method should handle exceptions of type `MethodArgumentNotValidException`.
    // This exception occurs when request parameters fail validation (e.g., @NotNull, @Size in DTOs).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Creates a HashMap to store validation errors.
        // The key will be the field name, and the value will be the error message.
        Map<String, String> errors = new HashMap<>();

        // Retrieves all validation errors from the exception object `ex` using `getBindingResult()`.
        // `getFieldErrors()` returns a list of errors related to specific fields.
        // Each error is mapped into the `errors` HashMap.
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );

        // Returns an HTTP 400 BAD REQUEST response.
        // The response body contains the `errors` map with field names and error messages.
        return ResponseEntity.badRequest().body(errors);
    }

    // Logger instance for logging warning messages
    private static final Logger log = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class) // Handles exceptions of type EmailAlreadyExistsException
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex) {

        // Logs a warning message when this exception occurs
        log.warn("Email address already exists {} ", ex.getMessage());

        // Creates a map to store the error response
        Map<String, String> errors = new HashMap<>();

        // Adds an error message to the response map
        errors.put("message", "Email address already exists");

        // Returns an HTTP 400 Bad Request response with the error message as the body
        return ResponseEntity.badRequest().body(errors);
    }

    // Exception handler for PatientNotFoundException
    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePatientNotFoundException(
            PatientNotFoundException ex) {

        // Logs a warning message when a patient is not found
        log.warn("Patient not found {}", ex.getMessage());

        // Creates a map to store the error response
        Map<String, String> errors = new HashMap<>();

        // Adds an error message to the response map
        errors.put("message", "Patient not found");

        // Returns an HTTP 400 Bad Request response with the error message as the response body
        return ResponseEntity.badRequest().body(errors);
    }
}

