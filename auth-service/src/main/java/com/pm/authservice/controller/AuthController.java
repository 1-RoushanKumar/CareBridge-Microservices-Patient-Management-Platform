package com.pm.authservice.controller;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.RegisterRequestDTO;
import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        // AuthService will now throw AuthenticationException for invalid credentials
        String token = String.valueOf(authService.authenticate(loginRequestDTO));
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Validated @RequestBody RegisterRequestDTO registerRequestDTO) {
        // Exceptions (UserAlreadyExistsException, NotARegisteredPatientException)
        // are now handled by GlobalExceptionHandler.
        authService.registerNewUser(registerRequestDTO);
        return ResponseEntity.status(201).build(); // 201 Created
    }

    @Operation(summary = "Validate Token")
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // This case might be better handled by a custom security filter or by allowing JwtAuthFilter
            // to set the response status. For now, returning UNAUTHORIZED here is fine.
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        // AuthService.validateToken should throw exceptions on failure, not return boolean.
        // This makes the controller cleaner and aligns with global exception handling.
        authService.validateToken(token); // Will throw JwtException or UsernameNotFoundException on failure
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user details by ID (Admin only)")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        UserResponseDTO userResponseDTO = authService.getUserDetailsById(id);
        return ResponseEntity.ok(userResponseDTO);
    }
}