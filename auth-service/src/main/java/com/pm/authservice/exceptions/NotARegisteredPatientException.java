package com.pm.authservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotARegisteredPatientException extends RuntimeException {
    public NotARegisteredPatientException(final String message) {
        super(message);
    }
}