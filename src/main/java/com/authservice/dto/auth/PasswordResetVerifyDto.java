package com.authservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for verifying reset code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Code is required")
    @Size(min = 6, max = 6, message = "Code must be 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits")
    private String code;
}