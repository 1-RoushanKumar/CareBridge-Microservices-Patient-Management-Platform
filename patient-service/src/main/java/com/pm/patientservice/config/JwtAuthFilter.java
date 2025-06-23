package com.pm.patientservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter implements InitializingBean {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    @Override
    public void afterPropertiesSet() throws ServletException {
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

                if (this.secretKey == null) {
                    throw new IllegalStateException("JWT secret key has not been initialized.");
                }

                Claims claims = Jwts.parser()
                        .setSigningKey(this.secretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();

                List<?> rolesObject = claims.get("roles", List.class);
                List<GrantedAuthority> authorities;

                if (rolesObject != null) {
                    authorities = rolesObject.stream()
                            .map(Object::toString)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    authorities = List.of();
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                logger.error("JWT authentication failed: " + e.getClass().getName() + ": " + e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"message\": \"Invalid or expired JWT token.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}