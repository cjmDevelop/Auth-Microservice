package com.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VerificationRequest.java
 * DTO for email/sms verification code validation
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    
    @NotBlank(message = "Verification code is required")
    private String code;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
