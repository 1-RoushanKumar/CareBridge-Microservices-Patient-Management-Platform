package com.pm.authservice.client;

import com.pm.authservice.dto.PatientResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class PatientServiceClient {

    private final WebClient webClient;

    // Constructor to inject WebClient and base URL
    public PatientServiceClient(@Value("${patient-service.base-url}") String patientServiceBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(patientServiceBaseUrl)
                .build();
    }

    /**
     * Fetches patient details by email from the patient-service.
     * @param email The email of the patient to find.
     * @return An Optional containing PatientResponseDTO if found, empty Optional if not found.
     * @throws RuntimeException if there's a client-side error other than 404.
     */
    public Optional<PatientResponseDTO> getPatientByEmail(String email) {
        try {
            PatientResponseDTO patientDTO = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/by-email") // ✅ Keep this
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .bodyToMono(PatientResponseDTO.class)
                    .block();

            return Optional.ofNullable(patientDTO);
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (WebClientResponseException.Conflict e) { // ✅ Add this specifically
            System.err.println("Conflict error calling patient-service: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return Optional.empty(); // or throw a specific exception
        } catch (WebClientResponseException e) {
            System.err.println("Error calling patient-service: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to connect to patient service: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error calling patient-service: " + e.getMessage());
            throw new RuntimeException("Unexpected error during patient service call: " + e.getMessage(), e);
        }
    }
}
