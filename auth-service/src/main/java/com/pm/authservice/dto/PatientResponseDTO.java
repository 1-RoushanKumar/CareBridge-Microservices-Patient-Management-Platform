package com.pm.authservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private String contact;
    private String gender;
    private String emergencyContact;
    private LocalDate registeredDate;
}
