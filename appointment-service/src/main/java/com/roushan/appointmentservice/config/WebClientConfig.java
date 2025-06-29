// src/main/java/com/roushan/appointmentservice/config/WebClientConfig.java (new file)
package com.roushan.appointmentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${doctorService.baseUrl}") // Define this in application.properties
    private String doctorServiceBaseUrl;

    @Bean
    public WebClient doctorServiceWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(doctorServiceBaseUrl).build();
    }
}