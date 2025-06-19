package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.RegisterRequestDTO;
import com.pm.authservice.model.User;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));

        return token;
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token); // Calls JwtUtil to validate the token
            return true; // Token is valid
        } catch (JwtException e) {
            return false; // Token is invalid
        }
    }

    public void registerNewUser(RegisterRequestDTO registerRequestDTO) {
        // Check if user with this email already exists
        if (userService.findByEmail(registerRequestDTO.getEmail()).isPresent()) {
            // You should throw a specific exception here, e.g., UserAlreadyExistsException
            // For simplicity, throwing a generic RuntimeException for now.
            throw new RuntimeException("User with email " + registerRequestDTO.getEmail() + " already exists.");
        }

        User newUser = new User();
        newUser.setEmail(registerRequestDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword())); // Encode password
        newUser.setRole(registerRequestDTO.getRole() != null ? registerRequestDTO.getRole() : "USER"); // Default role

        userService.saveUser(newUser); // Save user via UserService
    }
}
