package com.pm.patientservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey; // Import for SecretKey

// JwtAuthFilter.java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey; // Declare SecretKey

    // Initialize the SecretKey once when the bean is created
    @Override
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();
        // Ensure jwtSecret is not null or empty before creating the key
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalArgumentException("JWT secret must not be null or empty.");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);

                // Ensure secretKey is initialized
                if (this.secretKey == null) {
                    // This should ideally not happen if afterPropertiesSet is called correctly by Spring
                    // but as a failsafe, re-initialize if somehow null.
                    this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                }

                // --- CORRECTED FIX START ---
                Claims claims = Jwts.parser()
                        .setSigningKey(this.secretKey) // Use the pre-initialized SecretKey
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                // --- CORRECTED FIX END ---

                String email = claims.getSubject(); // email or username
                // It's safer to check if "roles" claim exists and is a List
                List<?> rolesObject = claims.get("roles", List.class); // Get as List.class
                List<GrantedAuthority> authorities;

                if (rolesObject != null && !rolesObject.isEmpty()) {
                    authorities = rolesObject.stream()
                            .map(Object::toString) // Convert each element to String
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    authorities = List.of(); // No roles or empty list
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                System.err.println("JWT authentication failed: " + e.getClass().getName() + ": " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}