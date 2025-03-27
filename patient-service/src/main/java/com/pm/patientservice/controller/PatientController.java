package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("")
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {
        List<PatientResponseDTO> patients = patientService.getPatients();
        return ResponseEntity.ok().body(patients);
    }
//    Why Use ResponseEntity Instead of Direct List Return?
//    More Control Over HTTP Responses Allows setting custom status codes (e.g., NO_CONTENT when no data is available).
//    Consistent API Responses – Ensures uniform response structures across different endpoints.
//    Setting HTTP Headers – Enables adding custom headers (e.g., security, CORS).
//    Handling Edge Cases – Can explicitly return NO_CONTENT instead of an empty list.
//    Better Exception Handling – Helps return meaningful error responses instead of just throwing exceptions.
}
