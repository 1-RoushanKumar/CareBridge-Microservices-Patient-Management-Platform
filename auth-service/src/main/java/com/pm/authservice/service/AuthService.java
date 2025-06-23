package com.pm.authservice.service;

import com.pm.authservice.client.PatientServiceClient;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.RegisterRequestDTO;
import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.dto.PatientResponseDTO;
import com.pm.authservice.exceptions.NotARegisteredPatientException;
import com.pm.authservice.exceptions.UserAlreadyExistsException;
import com.pm.authservice.exceptions.UserNotFoundException;
import com.pm.authservice.mapper.UserMapper;
import com.pm.authservice.model.User;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PatientServiceClient patientServiceClient;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, PatientServiceClient patientServiceClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.patientServiceClient = patientServiceClient;
    }

    public String authenticate(LoginRequestDTO loginRequestDTO) {
        User user = userService.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        log.info("User {} authenticated successfully.", user.getEmail());
        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getPatientUuid());
    }

    public void validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);

            UserDetails userDetails = userService.loadUserByUsername(username); // This throws UsernameNotFoundException

            if (!jwtUtil.validateToken(token, userDetails)) {
                throw new JwtException("Token validation failed for user: " + username);
            }
            log.info("Token validated successfully for user: {}", username);
        } catch (UsernameNotFoundException e) {
            log.warn("Validation Error: User from token not found - {}", e.getMessage());
            throw e; // Rethrow to be handled by GlobalExceptionHandler
        } catch (JwtException e) {
            log.warn("Validation Error: JWT invalid - {}", e.getMessage());
            throw e; // Rethrow to be handled by GlobalExceptionHandler
        } catch (Exception e) {
            log.error("Validation Error: An unexpected error occurred - {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during token validation.", e); // General error
        }
    }

    public void registerNewUser(RegisterRequestDTO registerRequestDTO) {
        if (userService.findByEmail(registerRequestDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email '" + registerRequestDTO.getEmail() + "' already exists.");
        }

        User newUser = new User();
        newUser.setEmail(registerRequestDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));

        String requestedRole = registerRequestDTO.getRole() != null && !registerRequestDTO.getRole().isBlank()
                ? registerRequestDTO.getRole().toUpperCase() : "PATIENT";
        newUser.setRole(requestedRole);

        if ("PATIENT".equals(requestedRole)) {
            Optional<PatientResponseDTO> patientOptional = patientServiceClient.getPatientByEmail(registerRequestDTO.getEmail());

            if (patientOptional.isPresent()) {
                newUser.setPatientUuid(patientOptional.get().getId());
                log.info("Linked new patient user {} to existing patientId: {}", newUser.getEmail(), newUser.getPatientUuid());
            } else {
                throw new NotARegisteredPatientException("User with email '" + registerRequestDTO.getEmail() + "' is not registered as a patient in the system. Please register as a patient first.");
            }
        } else {
            // For ADMIN or other roles, if a patientUuid is not relevant, it remains null.
            log.info("Registering user {} with role {}. No patient UUID linkage required.", newUser.getEmail(), requestedRole);
        }

        userService.saveUser(newUser);
        log.info("User {} registered successfully with role {}", newUser.getEmail(), newUser.getRole());
    }

    public UserResponseDTO getUserDetailsById(UUID id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return UserMapper.toDTO(user);
    }
}