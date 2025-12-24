package com.authservice.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.authservice.model.AppSource;
import com.authservice.model.User;
import com.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService{

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Username can be either:
        // 1. "email|appSource" (e.g., "user@example.com|RANDOM_WRITES")
        // 2. "email" (backwards compatibility)

        String email;
        AppSource appSource = null;

        if (username.contains("|")) {
            // Parse email and appSource from "email|appSource"
            String[] parts = username.split("\\|");
            email = parts[0];
            if (parts.length > 1) {
                try {
                    appSource = AppSource.valueOf(parts[1]);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid app source in username: {}", parts[1]);
                    throw new UsernameNotFoundException("Invalid app source: " + parts[1]);
                }
            }
        } else {
            // Backwards compatibility: treat as email only
            email = username;
        }

        // Query by email and appSource if available
        Optional<User> user;
        if (appSource != null) {
            log.debug("Loading user by email and appSource: {} | {}", email, appSource);
            user = userRepository.findByEmailAndAppSource(email, appSource);
        } else {
            log.debug("Loading user by email only: {}", email);
            user = userRepository.findByEmail(email);
        }

        if(user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user.get();
    }
}
