package com.pm.authservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // This will make Spring return a 400 status
public class NotARegisteredPatientException extends RuntimeException {
    public NotARegisteredPatientException(String message) {
        super(message);
    }
}
