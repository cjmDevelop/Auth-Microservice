package com.authservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for account deletion request
 * User must confirm with password for security
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required for verification")
    private String password;

    // Optional: reason for deletion (for analytics)
    private String reason;
}
