package com.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 * 
 * JWT-based stateless authentication - CSRF not needed
 * 
 * Why CSRF is disabled:
 * - JWT tokens in Authorization headers (not cookies)
 * - Browsers do not auto-send Authorization headers
 * - CSRF attacks only work with automatic credential submission
 * - This setup is inherently CSRF-safe
 * 
 * Security features enabled:
 * ✅ JWT authentication
 * ✅ Stateless session management
 * ✅ CORS protection (configured in CorsConfig)
 * ✅ BCrypt password hashing
 * ✅ Role-based access control
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                // OPTIONS requests (CORS preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Health check endpoints (for keep-alive, no auth needed)
                .requestMatchers("/health", "/ping").permitAll()

                // Protected auth endpoints (require authentication)
                .requestMatchers(HttpMethod.DELETE, "/api/auth/account").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/account/can-delete").authenticated()

                // Public auth endpoints (no authentication needed)
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/resend-verification-email").permitAll()

                // Password reset endpoints (no authentication needed)
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-reset-code").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/resend-reset-code").permitAll()

                // Notes endpoints (protected - requires valid JWT)
                .requestMatchers("/api/notes/**").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Enable CORS (uses CorsFilter bean from CorsConfig)
            .cors(cors -> cors.configure(http))

            // CSRF disabled - safe for JWT-based APIs
            // (JWT in Authorization header = no CSRF risk)
            .csrf(csrf -> csrf.disable())

            // Add JWT filter before Spring Security's default auth filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}