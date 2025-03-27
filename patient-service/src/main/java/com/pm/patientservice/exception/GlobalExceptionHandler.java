package com.pm.patientservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

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
}

