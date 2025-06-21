package com.pm.authservice.model;

import jakarta.persistence.*;
import java.util.UUID; // Import UUID

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // This will store the single role string (e.g., "ROLE_PATIENT", "ROLE_ADMIN")

    @Column(name = "patient_uuid", unique = true) // NEW FIELD: Link to patient-service's Patient ID
    private UUID patientUuid;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getPatientUuid() { // NEW GETTER
        return patientUuid;
    }

    public void setPatientUuid(UUID patientUuid) { // NEW SETTER
        this.patientUuid = patientUuid;
    }
}