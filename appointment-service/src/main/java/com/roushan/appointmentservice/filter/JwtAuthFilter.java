package com.roushan.appointmentservice.filter;

import com.roushan.appointmentservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check for JWT in Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Extract the token after "Bearer "

        try {
            userEmail = jwtUtil.extractUsername(jwt);

            // 2. If username is valid and no authentication is currently set in context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate the token's basic properties (e.g., expiration).
                // API Gateway should have already fully validated signature with Auth Service.
                if (jwtUtil.validateToken(jwt)) {
                    // Extract roles and patient_id
                    List<String> roles = jwtUtil.extractRoles(jwt);
                    UUID patientId = jwtUtil.extractPatientId(jwt);

                    // Create GrantedAuthorities from roles
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Create an Authentication object
                    // We use UsernamePasswordAuthenticationToken for simplicity with roles.
                    // For patient_id, we'll store it directly on the Authentication object or a custom Principal.
                    // Option 1: Store patient_id as a "detail"
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail, // Principal: username (email)
                            null,      // Credentials: not needed after token validation
                            authorities // Authorities (roles)
                    );

                    // Add patientId to the authentication details. This makes it accessible later.
                    // We'll create a custom JwtAuthenticationDetails class to hold this.
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Option 2: Use a custom Principal object if you need patientId directly on the principal
                    // We will implement this more robustly in a later step
                    // For now, let's stick with storing it in details and extracting from there.

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Store patientId and roles in request attributes for easier access in controllers/services
                    // This is a common pattern for data extracted from JWT that isn't part of UserDetails
                    request.setAttribute("userEmail", userEmail);
                    request.setAttribute("userRoles", roles);
                    request.setAttribute("patientId", patientId); // Patient UUID
                }
            }
        } catch (Exception e) {
            // Log the error but continue the filter chain.
            // Invalid tokens will result in no authentication being set,
            // which Spring Security will then handle (e.g., 401 Unauthorized for protected resources).
            logger.error("JWT authentication failed: {}");
            // It's often good practice to clear the context on failure if it was partially set
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}