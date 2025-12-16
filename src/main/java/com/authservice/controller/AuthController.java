package com.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authservice.dto.auth.AccountDeletionDto;
import com.authservice.dto.auth.AuthResponseDto;
import com.authservice.dto.auth.LoginRequestDto;
import com.authservice.dto.auth.PasswordResetRequestDto;
import com.authservice.dto.auth.RegisterRequestDto;
import com.authservice.dto.auth.VerificationRequestDto;
import com.authservice.service.AccountDeletionService;
import com.authservice.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) {
        try {
            AuthResponseDto response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed. Please try again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerificationRequestDto verReqDto) {
        try {
            AuthResponseDto response = authService.verifyEmail(verReqDto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email verification failed. Please try again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            authService.resendEmailVerification(request.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Verification code resent to your email");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to resend verification email. Please try again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            AuthResponseDto response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed. Please try again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            AuthResponseDto response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to refresh token. Please login again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==================== DELETE ACCOUNT ENDPOINTS ====================

    /**
     * DELETE /api/auth/account
     * Delete current user's account permanently
     *
     * Security:
     * - User must be authenticated (JWT required)
     * - Password verification required
     * - Cannot be undone
     */
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
        @Valid @RequestBody AccountDeletionDto request,
        Authentication authentication
    ) {
        try {
            // Verify authenticated user matches request email
            String authenticatedEmail = authentication.getName();

            if (!authenticatedEmail.equals(request.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You can only delete your own account");
                return ResponseEntity.badRequest().body(error);
            }

            // Delete account
            accountDeletionService.deleteAccount(request);

            // Success response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account deleted successfully");
            response.put("email", request.getEmail());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete account. Please try again.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * GET /api/auth/account/can-delete
     * Check if current user can delete their account
     * (Optional endpoint for business rules)
     */
    @GetMapping("/account/can-delete")
    public ResponseEntity<?> canDeleteAccount(Authentication authentication) {
        try {
            String email = authentication.getName();
            boolean canDelete = accountDeletionService.canDeleteAccount(email);

            Map<String, Object> response = new HashMap<>();
            response.put("canDelete", canDelete);
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check account status");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
