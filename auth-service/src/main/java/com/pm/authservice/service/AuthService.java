// auth-service/src/main/java/com/pm/authservice/service/AuthService.java
package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.RegisterRequestDTO;
import com.pm.authservice.dto.UserResponseDTO;
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

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<User> userOptional = userService.findByEmail(loginRequestDTO.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                return Optional.of(jwtUtil.generateToken(user.getEmail(), user.getRole()));
            }
        }
        return Optional.empty();
    }

    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);

            User user = userService.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Assuming your `User` model implements `UserDetails`.
            // If not, you'd need to create a `UserDetails` object from your `User` here.
            // Example if User doesn't implement UserDetails:
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(), // Password not used for validation here but required by constructor
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
        newUser.setRole(registerRequestDTO.getRole() != null && !registerRequestDTO.getRole().isBlank()
                ? registerRequestDTO.getRole().toUpperCase() : "USER");

        userService.saveUser(newUser);
    }

    public UserResponseDTO getUserDetailsById(UUID id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return UserMapper.toDTO(user);
    }
}