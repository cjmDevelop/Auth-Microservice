package com.authservice.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.authservice.service.JWTService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT Authentication Filter
 * 
 * This filter intercepts every http request to check for JWT tokens.
 * This runs before controllers, validating authentication
 * 
 * Flow:
 * 1. Extract JWT from Authorization header
 * 2. Validate the token
 * 3. Load user details
 * 4. Set authentication in Spring Security context
 * 5. Pass request to next filter/controller
 */
@Component // Makes this a Spring bean so it can be injected
@RequiredArgsConstructor // Lombok creates constructor for final fields
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // The service to validate and extract data from JWT tokens
    private final JWTService jwtService;

    // Service to load user details from database
    private final UserDetailsService userDetailsService;

    /**
     * This method will run for every request
     * Check authentication before reaching controllers
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

                System.out.println("🔍JWT Filter running for: " + request.getRequestURI());
                System.out.println("🔍 Method: " + request.getMethod());

                //Skipping JWT processing for OPTIONS requests (CORS preflight)
                if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    System.out.println("⏭ Skipping OPTIONS request");
                    filterChain.doFilter(request, response);
                    return;
                }

        // Step 1. Extract the Authorization header
        // Format expected: "Bearer eyJhbGciOiJ..."
        final String authHeader = request.getHeader("Authorization");
        System.out.println("📝 Auth header: " + authHeader);

        // Step 2: Check if header exists and starts with "Bearer "
        // If not, skip JWT processing
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // Exit early
        }

        /**
         * Step 3: Extracting the actual JWT token from the "Bearer " prefix
         */
        final String jwt = authHeader.substring(7);

        /**
         * Step 4: Extract username (email) from JWT token
         * JWTService decodes the token and gets the "subject" (username)
         */
        final String userEmail = jwtService.extractUsername(jwt);

        /**
         * Step 5: Check if we have a username and user is not already authenticated
         * SecurityContextHolder.getContext().getAuthentication() returns current auth,
         * if null user is not authenticated.
         */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            /**
             * Step 6: Load user details from database using email
             * This calls CustomUserDetailsService.loadUserByUsername()
             */
            UserDetails userdetails = this.userDetailsService.loadUserByUsername(userEmail);

            /**
             * Step 7: Validate the JWT token for this user
             * Checks signature, expiration, and if token belongs to this user
             */
            if (jwtService.isTokenValid(jwt, userdetails)) {

                /**
                 * Step 8: Create authentication token
                 * This tells Spring Security the user is authenticated
                 */
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userdetails,
                        null,
                        userdetails.getAuthorities());

                /**
                 * Step 9: Add request details (IP address, session, etc.)
                 * This is metadata about the request
                 */
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                /**
                 * Step 10: Set authentication in SecurityContext in order for Spring
                 * security to know this user is authenticated
                 * Controllers can access this via SecurityContextHolder
                 */
                SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("🔐Authentication SET in SecurityContext");
            } else {
                System.out.println("❌Token is invalid");
            }
        } else {
            System.out.println("⚠️User Already authenticated or email is null");
        }

        /**
         * Step 11: Continue the filter chain
         * Pass request to next filter or to the controller
         */
        filterChain.doFilter(request, response);

    }
}
