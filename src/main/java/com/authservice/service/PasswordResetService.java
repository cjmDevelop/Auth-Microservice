package com.authservice.service;

import com.authservice.model.PasswordResetToken;
import com.authservice.model.User;
import com.authservice.repository.PasswordResetTokenRepository;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ResendEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;

    /**
     * Step 1: User requests password reset - generate and send code
     * 
     * OPTIMIZED: Email is sent asynchronously, API returns immediately
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("🔐 Password reset requested for: {}", email);
        
        // Find user by email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        // Check if user's email is verified
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before resetting password");
        }

        // Delete any old reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate random 6-digit code
        String code = generateResetCode();

        // Create and save new token
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user)
            .code(code)
            .expiryDate(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
            .used(false)
            .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("✅ Reset token saved for: {}", email);

        // Send email asynchronously (doesn't block API response)
        // The @Async annotation in ResendEmailService handles this
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), code);
        
        log.info("📧 Password reset email queued for: {}", email);
        // API returns immediately, email sends in background!
    }

    /**
     * Step 2: Verify the reset code user entered
     */
    public boolean verifyResetCode(String email, String code) {
        log.info("🔍 Verifying reset code for: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByUserAndCode(user, code)
            .orElse(null);

        // Check if token exists
        if (resetToken == null) {
            log.warn("❌ Invalid code for: {}", email);
            return false;
        }

        // Check if expired
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("⏰ Expired code for: {}", email);
            return false;
        }

        // Check if already used
        if (resetToken.isUsed()) {
            log.warn("♻️ Code already used for: {}", email);
            return false;
        }

        log.info("✅ Code verified for: {}", email);
        return true;
    }

    /**
     * Step 3: Reset the password after code is verified
     * 
     * OPTIMIZED: Confirmation email is sent asynchronously
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        log.info("🔐 Resetting password for: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByUserAndCode(user, code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid reset code"));

        // Verify code is still valid
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset code has expired");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset code has already been used");
        }

        // Validate new password
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Update user's password (encrypted!)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used so it can't be reused
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("✅ Password reset successfully for: {}", email);

        // Send confirmation email asynchronously (doesn't block API response)
        emailService.sendPasswordChangedConfirmation(user.getEmail(), user.getFirstName());
        
        log.info("📧 Password changed confirmation queued for: {}", email);
        // API returns immediately, email sends in background!
    }

    /**
     * Resend code if user didn't receive it
     * 
     * OPTIMIZED: Email is sent asynchronously
     */
    @Transactional
    public void resendResetCode(String email) {
        log.info("🔄 Resending reset code for: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        // Delete old tokens
        passwordResetTokenRepository.deleteByUser(user);
        
        // Start fresh password reset (email sent asynchronously)
        initiatePasswordReset(email);
    }

    /**
     * Generate a random 6-digit code
     */
    private String generateResetCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Range: 100000-999999
        return String.valueOf(code);
    }

    /**
     * Cleanup expired tokens (optional - can run as scheduled job)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.info("🧹 Cleaned up expired password reset tokens");
    }
}