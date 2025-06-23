package com.pm.authservice.client;

import com.pm.authservice.dto.PatientResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class PatientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceClient.class);

    private final WebClient webClient;

    public PatientServiceClient(@Value("${patient-service.base-url}") String patientServiceBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(patientServiceBaseUrl)
                .build();
    }

    public Optional<PatientResponseDTO> getPatientByEmail(String email) {
        try {
            PatientResponseDTO patientDTO = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/patients/by-email") // Corrected path to include /patients if base URL is just host:port
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .bodyToMono(PatientResponseDTO.class)
                    .block();

            return Optional.ofNullable(patientDTO);
        } catch (WebClientResponseException.NotFound e) {
            // Patient not found in patient-service, this is an expected scenario for new registrations
            log.info("Patient with email {} not found in patient-service.", email);
            return Optional.empty();
        } catch (WebClientResponseException.BadRequest e) {
            // e.g., validation error on email from patient-service, or other 400 issues
            log.error("Bad request error from patient-service for email {}: Status {} - Body {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Patient service returned a bad request: " + e.getResponseBodyAsString(), e);
        } catch (WebClientResponseException.Conflict e) {
            // This might indicate a problem on the patient service if a conflict occurs when fetching by email.
            // Re-throwing as an error to indicate an unexpected state during lookup.
            log.error("Conflict error from patient-service for email {}: Status {} - Body {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Patient service conflict during lookup: " + e.getResponseBodyAsString(), e);
        } catch (WebClientResponseException e) {
            // Catch any other WebClientResponseException (e.g., 5xx errors)
            log.error("Error calling patient-service for email {}: Status {} - Body {}", email, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to connect to patient service: " + e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected errors
            log.error("Unexpected error during patient service call for email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during patient service call: " + e.getMessage(), e);
        }
    }
}