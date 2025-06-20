// com.pm.apigateway.filter/JwtValidationGatewayFilterFactory.java
package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(
            WebClient.Builder webClientBuilder,
            @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }
    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Call auth service to validate the token
            return webClient.get()
                    .uri("/validate")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(responseEntity -> {
                        // If auth service returns 2xx, continue the filter chain
                        if (responseEntity.getStatusCode().is2xxSuccessful()) {
                            return chain.filter(exchange);
                        } else {
                            // If auth service returns non-2xx (e.g., 401, 403), set status and complete
                            exchange.getResponse().setStatusCode(responseEntity.getStatusCode());
                            return exchange.getResponse().setComplete();
                        }
                    })
                    .onErrorResume(e -> {
                        // Handle errors from WebClient (e.g., connection refused, non-2xx status codes)
                        // Log the error for debugging purposes
                        System.err.println("Error during JWT validation: " + e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // Or HttpStatus.INTERNAL_SERVER_ERROR
                        return exchange.getResponse().setComplete();
                    });
        });
    }
}