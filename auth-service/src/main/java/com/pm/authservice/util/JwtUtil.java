package com.pm.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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
public class JwtUtil implements InitializingBean { // Implement InitializingBean for secret key setup
    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey; // Changed to SecretKey

    private final long JWT_EXPIRATION_MS = 1000 * 60 * 60 * 10; // 10 hours
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalArgumentException("JWT secret must not be null or empty.");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        logger.info("JWTUtil initialized: Secret key loaded successfully.");
    }

    // Removed constructor that took String secret to use @Value and afterPropertiesSet

    public String generateToken(String email, String role, UUID patientUuid) {
        // Ensure role is prefixed with "ROLE_"
        String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        Claims claims = Jwts.claims()
                .subject(email)
                .add("roles", List.of(prefixedRole))
                .build(); // Build claims

        if (patientUuid != null) {
            claims.put("patient_id", patientUuid.toString());
        }

        Date issuedAt = new Date(System.currentTimeMillis());
        Date expiration = new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
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
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature.", e); // Re-throw specific exception
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new JwtException("Invalid JWT token.", e); // Re-throw specific exception
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            throw new JwtException("Unexpected error validating token.", e); // Re-throw generic exception
        }
    }
}