// src/main/java/com/roushan/appointmentservice/util/JwtUtil.java
package com.roushan.appointmentservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders; // Keep this import
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key secretKey;
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // --- ADD THIS LOGGING LINE ---
        logger.info("Appointment-Service JWT Secret (raw): {}", secret);
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        logger.info("Appointment-Service JWT Secret Key (derived): {}", secretKey.getEncoded()); // This will print bytes, useful for comparison
        // --- END ADDITION ---
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (Exception e) {
            logger.error("Error parsing JWT: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing JWT", e);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public UUID extractPatientId(String token) {
        String patientIdString = extractClaim(token, claims -> claims.get("patient_id", String.class));
        return patientIdString != null ? UUID.fromString(patientIdString) : null;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}