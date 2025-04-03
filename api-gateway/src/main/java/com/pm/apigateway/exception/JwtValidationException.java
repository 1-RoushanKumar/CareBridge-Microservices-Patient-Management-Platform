package com.pm.apigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for handling JWT validation-related exceptions.
 * This class ensures that unauthorized access attempts are properly handled.
 */
@RestControllerAdvice // Marks this class as a centralized exception handler for REST controllers.
public class JwtValidationException {

    /**
     * Handles cases where the authentication service responds with an Unauthorized (401) status.
     *
     * @param exchange The current web request context.
     * @return A Mono<Void> indicating that the response is complete.
     */
    @ExceptionHandler(WebClientResponseException.Unauthorized.class)
    public Mono<Void> handleUnauthorizedException(ServerWebExchange exchange) {
        // Set the HTTP response status to 401 Unauthorized.
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        // Complete the response without returning any body.
        return exchange.getResponse().setComplete();
    }
}
