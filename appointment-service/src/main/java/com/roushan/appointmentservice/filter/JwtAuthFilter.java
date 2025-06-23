package com.roushan.appointmentservice.filter;

import com.roushan.appointmentservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        String userEmail = null;
        List<String> roles = null;
        UUID patientId = null;
        UUID doctorId = null; // New: To extract doctorId

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtUtil.extractUsername(jwt);
            roles = jwtUtil.extractRoles(jwt);
            patientId = jwtUtil.extractPatientId(jwt);
            doctorId = jwtUtil.extractDoctorId(jwt); // New: Extract doctorId

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // In a real application, you might fetch UserDetails from a UserDetailsService here
                // and then validate the token against those UserDetails.
                // For simplicity, we are just validating the token structurally and checking expiration.
                if (jwtUtil.validateToken(jwt)) {
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null, // Credentials usually null for JWT as they are already validated
                            authorities
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Set attributes for downstream use in controllers/services
                    request.setAttribute("userEmail", userEmail);
                    request.setAttribute("userRoles", roles);
                    if (patientId != null) {
                        request.setAttribute("patientId", patientId);
                    }
                    if (doctorId != null) {
                        request.setAttribute("doctorId", doctorId); // New: Set doctorId
                    }
                    logger.debug("Successfully authenticated user: {} with roles: {}", userEmail, roles);
                } else {
                    logger.warn("JWT token is invalid or expired for user: {}", userEmail);
                }
            }
        } catch (io.jsonwebtoken.JwtException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Explicitly set status
            response.getWriter().write("Invalid or expired token.");
            return; // Stop processing and return immediately
        } catch (Exception e) {
            logger.error("An unexpected error occurred during JWT authentication: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Explicitly set status
            response.getWriter().write("Authentication processing error.");
            return; // Stop processing and return immediately
        }

        filterChain.doFilter(request, response);
    }
}