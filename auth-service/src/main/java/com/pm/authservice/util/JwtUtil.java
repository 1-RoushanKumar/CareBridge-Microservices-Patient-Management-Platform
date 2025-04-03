package com.pm.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key secretKey;

    //here i passing jwt_secret from environment variable
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT token by checking its signature and structure.
     * @param token The JWT token to validate.
     * @throws JwtException If the token is invalid or its signature does not match.
     */
    public void validateToken(String token) {
        try {
            // Parse the token and verify the signature using the secret key
            Jwts.parser()
                    .verifyWith((SecretKey) secretKey) // Uses the secret key to verify the token's signature
                    .build()
                    .parseSignedClaims(token); // Parses and verifies the signed JWT

        } catch (SignatureException e) {
            // If the token's signature does not match, throw an exception
            throw new JwtException("Invalid JWT signature");
        } catch (JwtException e) {
            // Any other JWT-related exception (expired, malformed, etc.)
            throw new JwtException("Invalid JWT token");
        }
    }
}
