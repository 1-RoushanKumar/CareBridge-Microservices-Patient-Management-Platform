package com.roushan.appointmentservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean; // New import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil implements InitializingBean { // Implement InitializingBean

    @Value("${jwt.secret}")
    private String jwtSecret; // Use @Value to inject the secret

    private SecretKey secretKey; // Changed to SecretKey type
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // No need for a constructor that takes secret if using @Value and InitializingBean
    public JwtUtil() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            logger.error("JWT secret property 'jwt.secret' is not set or is empty.");
            throw new IllegalArgumentException("JWT secret must be configured.");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        logger.info("JWTUtil initialized: Secret key derived."); // Do not log the key itself
    }


    private Claims extractAllClaims(String token) {
        // Use parserBuilder for modern JJWT API
        return Jwts.parser()

                .verifyWith((SecretKey) secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (JwtException e) { // Catch JJWT specific exceptions
            logger.error("Failed to extract claim from token: {}", e.getMessage());
            throw e; // Re-throw to be handled by filter/exception handler
        } catch (Exception e) {
            logger.error("An unexpected error occurred while extracting claim: {}", e.getMessage(), e);
            throw new JwtException("Unexpected error during claim extraction.", e); // Wrap in JwtException
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        // Ensure that roles are stored as a List in the JWT claims
        // If roles can be single String, handle accordingly
        Object roles = extractClaim(token, claims -> claims.get("roles"));
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of(); // Return empty list if not a list
    }

    public UUID extractPatientId(String token) {
        String patientIdString = extractClaim(token, claims -> claims.get("patient_id", String.class));
        return patientIdString != null ? UUID.fromString(patientIdString) : null;
    }

    // New method to extract Doctor ID from token
    public UUID extractDoctorId(String token) {
        String doctorIdString = extractClaim(token, claims -> claims.get("doctor_id", String.class));
        return doctorIdString != null ? UUID.fromString(doctorIdString) : null;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Simple validation for internal filter use
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException e) {
            // This is already logged by extractClaim methods if they throw
            logger.warn("Token validation failed due to JWT exception: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return false;
        }
    }
}