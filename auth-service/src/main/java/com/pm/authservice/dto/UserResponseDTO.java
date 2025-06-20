package com.pm.authservice.dto;

import java.util.UUID;

public class UserResponseDTO {
    private UUID id;
    private String email;
    private String role;

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }
}