package com.pm.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final long JWT_EXPIRATION_MS = 1000 * 60 * 60 * 10; // 10 hours
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        logger.info("Auth-Service JWT Secret (raw): {}", secret);
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        logger.info("Auth-Service JWT Secret Key (derived): {}", secretKey.getEncoded());
    }

    // Modified method to include patientUuid
    public String generateToken(String email, String role, UUID patientUuid) {
        String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        // Start building claims, but don't call .build() yet
        io.jsonwebtoken.ClaimsBuilder claimsBuilder = Jwts.claims()
                .subject(email)
                .add("roles", List.of(prefixedRole));

        // Add patient_id claim directly to the builder if patientUuid is not null
        if (patientUuid != null) {
            claimsBuilder.add("patient_id", patientUuid.toString()); // Add to the builder, not the built Claims object
        }

        // Now, build the Claims object after all desired claims are added
        Claims claims = claimsBuilder.build();

        return Jwts.builder()
                .claims(claims) // Pass the fully constructed and immutable Claims object
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(secretKey)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // New method to extract patient_id from token
    public UUID extractPatientId(String token) {
        String patientIdString = extractClaim(token, claims -> claims.get("patient_id", String.class));
        return patientIdString != null ? UUID.fromString(patientIdString) : null;
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: " + e.getMessage());
            return false;
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("Unexpected error during token validation: " + e.getMessage());
            return false;
        }
    }
}