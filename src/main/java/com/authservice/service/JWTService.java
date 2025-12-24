package com.authservice.service;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.authservice.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/** JwtService - Handles JSON Web Token (JWT) creation and validation
 * 
 *  JWT Structure: HEADER.PAYLOAD.SIGNATURE
 *  - Header: Token type and algorithm
 *  - Payload: User data (claims)
 *  - Signature: Ensures token hasn't been compromised
 * 
 * Service for generating and validating JWT tokens.
 * Handles access tokens and refresh tokens with configurable expiration times.
 */
@Service
public class JWTService {
    /**
     * Used to sign tokens, so tokens can't be forged.
     */@Value("${jwt.secret}")
       private String secretKey;    

    /**
     * Access token time in milliseconds
     * Time is set in application.yml 86400000 = 24 hours
     * After this time, user needs to refresh or login again
     */@Value("${jwt.expiration}")
       private long jwtExpiration;

    /**
     * Refresh token time in milliseconds
     * Time is set in application.yml 86400000 = 24 hours
     * Used to get new access tokens without re-logging in
     */@Value("${jwt.refresh-expiration}")
       private long refreshExpiration;


  /**
   * Extract username (email) from a JWT token
   * @param token
   * @return The user's email address stored in the token
   */public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

  /**
   * Extract appSource from a JWT token
   * @param token
   * @return The appSource value stored in the token, or null if not present
   */public String extractAppSource(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("appSource", String.class);
    }

  /**
   * Generic method to extract any claim from a token
   * 
   * @param <T> - Generic type which allows for any data-type / object 
   * @param token - The JWT string
   * @param claimsResolver - Function that identifies which claim to extract
   * @return - The specific claim value
   * 
   * Example:
   * extractClaim(token, Claims::getSubject) -> returns String (username)
   * extractClaim(token, Claims::getExpiration) -> returns Date (expiry)
   * 
   */public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a basic JWT token with no extra claims
     * Only includes username and standard fields (issued time, expiry)
     * IMPORTANT: Also includes appSource to support multi-app architecture
     * @param userDetails
     * @return calls the overloaded version below with appSource claim
     */public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Add appSource to JWT claims if user is a User instance
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            claims.put("appSource", user.getAppSource().name());
        }

        return generateToken(claims, userDetails);
    }

    /**
     * Generate a JWT token with extra custom claims
     * @param extraClaims - Custom data to include (e.g., {"role": "ADMIN"})
     * @param userDetails - The logged in user's information
     * @return JWT token string
     * This is "full version" config which will allow for later custom data if needed.
     */public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generate a refresh token (lives longer than access token)
     * Used to get new access tokens without making user login again
     * Includes appSource for multi-app support
     * @param userDetails
     * @return refresh token
     */ public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Add appSource to refresh token claims
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            claims.put("appSource", user.getAppSource().name());
        }

        return buildToken(claims, userDetails, refreshExpiration);
    }

    /**
     * The actual token builder - creates the JWT string
     * @param extraClaims - Custom data to include in token
     * @param userDetails - User information (email etc.)
     * @param expiration - How long until token expires in milliseconds
     * @return JWT string 
     */private String buildToken(
        Map<String, Object> extraClaims,
        UserDetails userDetails,
        long expiration
    ) {
        return Jwts.builder()
        .claims(extraClaims)
        .subject(userDetails.getUsername()) // Error: Cannot make a static reference to the non-static method getUsername() from the type UserDetailsJava(603979977)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey())
        .compact();
    }

    /**
     * Validate a token (check if username matches and token isn't expired)
     * 
     * @param token - JWT string from frontend
     * @param userDetails - Current user from database
     * @return true if valid, false if invalid/expired
     * 
     * Checks that token belongs to this user and is still valid
     */public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if token expiratiom date has come/passed
     * @param token
     * @return true if expired, false if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    /**
     * * Extract the expiration date from token
     * 
     * @param token
     * @return Date when token expires
     */private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parse JWT token and extract all claims (all data inside)
     * 
     * @param token - JWT string
     * @return - Claims object containing all token data
     * 
     * Steps:
     * 1. Parse the JWT string
     * 2. Verify signature using secret key 
     * 3. Extract payload
     * 4. if signature doesnt match throws error
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser() 
                   .verifyWith(getSignInKey()) 
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    /**
     * Convert secret key string to cryptographic Key Object
     * 
     * @return SecretKey for signing/verifying tokens
     */private SecretKey getSignInKey() {
        byte [] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get token expiration time (used in API responses)
     * Frontend needs this to know when on refresh
     * @return Expiration time in milliseconds
     */public long getExpirationTime() {
        return jwtExpiration;
    }

}
