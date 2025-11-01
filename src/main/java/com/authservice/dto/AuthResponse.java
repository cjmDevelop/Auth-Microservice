package com.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * AuthResponse.java 
 * DTO for authentication responses.
 * Returns JWT access/refresh tokens and user information after successful login.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken; //Set the access token for user 
    private String refreshToken; //refresh token if needed
    private String tokenType; //Set which kind of token
    private Long expiresIn; //Expires in an amount of time
    private UserDto user; //UserDto object
}

