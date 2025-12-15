package com.authservice.controller;


import com.authservice.dto.auth.PasswordResetRequestDto;
import com.authservice.dto.auth.PasswordResetVerifyDto;
import com.authservice.dto.auth.PasswordUpdateDto;
import com.authservice.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/forgot-password
     * Step 1: User enters email, we send them a code
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset code sent to your email");
            response.put("email", request.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to send reset code. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/verify-reset-code
     * Step 2: User enters the code, we verify it's correct
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@Valid @RequestBody PasswordResetVerifyDto request) {
        try {
            boolean isValid = passwordResetService.verifyResetCode(
                request.getEmail(), 
                request.getCode()
            );
            
            if (isValid) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Code verified successfully");
                response.put("verified", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or expired code");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Verification failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/reset-password
     * Step 3: User enters new password, we update it
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordUpdateDto request) {
        try {
            passwordResetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update password. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/resend-reset-code
     * Optional: Resend code if user didn't receive it
     */
    @PostMapping("/resend-reset-code")
    public ResponseEntity<?> resendResetCode(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            passwordResetService.resendResetCode(request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Reset code resent to your email");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to resend code. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
