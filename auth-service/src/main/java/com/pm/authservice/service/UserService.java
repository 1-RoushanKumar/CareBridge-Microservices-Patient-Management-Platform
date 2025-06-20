package com.pm.authservice.service;

import com.pm.authservice.model.User;
import com.pm.authservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Import this
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import this
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
// Implement UserDetailsService
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    // Implement the loadUserByUsername method as required by UserDetailsService
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Retrieve the user from your repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Convert your custom User entity to Spring Security's UserDetails.
        // Spring Security's UserDetails expects a collection of GrantedAuthority.
        // Assuming your User has a 'role' field, you can convert it to a SimpleGrantedAuthority.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}