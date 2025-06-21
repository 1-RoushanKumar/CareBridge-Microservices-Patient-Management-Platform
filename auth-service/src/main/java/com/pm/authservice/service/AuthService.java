package com.pm.authservice.service;

import com.pm.authservice.client.PatientServiceClient; // Import the new client
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.RegisterRequestDTO;
import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.dto.PatientResponseDTO; // Import PatientResponseDTO
import com.pm.authservice.exceptions.NotARegisteredPatientException; // Import your custom exception
import com.pm.authservice.exceptions.UserAlreadyExistsException;
import com.pm.authservice.exceptions.UserNotFoundException;
import com.pm.authservice.mapper.UserMapper;
import com.pm.authservice.model.User;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PatientServiceClient patientServiceClient; // Inject PatientServiceClient

    // Update constructor to inject PatientServiceClient
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, PatientServiceClient patientServiceClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.patientServiceClient = patientServiceClient; // Assign it
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<User> userOptional = userService.findByEmail(loginRequestDTO.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                return Optional.of(jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getPatientUuid()));
            }
        }
        return Optional.empty();
    }

    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);

            User user = userService.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );

            return jwtUtil.validateToken(token, userDetails);

        } catch (UsernameNotFoundException e) {
            System.err.println("Validation Error: User from token not found - " + e.getMessage());
            return false;
        } catch (JwtException e) {
            System.err.println("Validation Error: JWT invalid - " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Validation Error: An unexpected error occurred - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void registerNewUser(RegisterRequestDTO registerRequestDTO) {
        if (userService.findByEmail(registerRequestDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + registerRequestDTO.getEmail() + " already exists.");
        }

        User newUser = new User();
        newUser.setEmail(registerRequestDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));

        // Normalize role string and set default
        String requestedRole = registerRequestDTO.getRole() != null && !registerRequestDTO.getRole().isBlank()
                ? registerRequestDTO.getRole().toUpperCase() : "PATIENT";
        newUser.setRole(requestedRole);

        // Logic to handle patientUuid based on role and patient-service lookup
        if ("PATIENT".equals(requestedRole)) {
            // Try to find the patient in patient-service
            Optional<PatientResponseDTO> patientOptional = patientServiceClient.getPatientByEmail(registerRequestDTO.getEmail());

            if (patientOptional.isPresent()) {
                // If patient found, set their UUID
                newUser.setPatientUuid(patientOptional.get().getId());
                System.out.println("DEBUG: Linked new patient user " + newUser.getEmail() + " to existing patientId: " + newUser.getPatientUuid());
            } else {
                // If patient not found in patient-service, throw custom exception
                throw new NotARegisteredPatientException("User with email " + registerRequestDTO.getEmail() + " is not registered as a patient in the system.");
            }
        }
        // For ADMIN or other roles, patientUuid remains null, which is the desired behavior.

        userService.saveUser(newUser);
    }

    public UserResponseDTO getUserDetailsById(UUID id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return UserMapper.toDTO(user);
    }
}