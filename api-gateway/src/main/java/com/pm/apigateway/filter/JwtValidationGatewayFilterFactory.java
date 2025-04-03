package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This class defines a custom Gateway Filter for validating JWT tokens in API Gateway.
 * It ensures that incoming requests have a valid JWT token before allowing them to proceed.
 */
@Component // Marks this class as a Spring component so that it is auto-detected during classpath scanning.
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient; // WebClient is used to make HTTP requests to the authentication service.

    /**
     * Constructor initializes the WebClient with the authentication service URL.
     *
     * @param webClientBuilder Used to build WebClient instances.
     * @param authServiceUrl   The base URL of the authentication service, injected from environment variable.
     */
    public JwtValidationGatewayFilterFactory(
            WebClient.Builder webClientBuilder,
            @Value("${auth.service.url}") String authServiceUrl) {

        // Configures the WebClient to communicate with the authentication service.
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    /**
     * This method applies the JWT validation filter to incoming requests.
     *
     * @param config Configuration object (not used in this implementation).
     * @return GatewayFilter that performs JWT validation.
     */
    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            // Extracts the Authorization header from the incoming request.
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // If the Authorization header is missing or does not start with "Bearer ", reject the request.
            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete(); // Terminates the request.
            }

            // Calls the authentication service to validate the token.
            return webClient.get()
                    .uri("/validate") // Calls the "/validate" endpoint of the auth service.
                    .header(HttpHeaders.AUTHORIZATION, token) // Passes the token to the auth service.
                    .retrieve() // Sends the request and expects a response.
                    .toBodilessEntity() // We only care about the status, not the response body.
                    .then(chain.filter(exchange)); // If validation is successful, continue the request.
        });
    }
}
