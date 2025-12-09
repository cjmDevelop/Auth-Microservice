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
     */
    @Transactional
    public void initiatePasswordReset(String email) {
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

        // Send email with the code
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), code);
        
        log.info("Password reset initiated for user: {}", email);
    }

    /**
     * Step 2: Verify the reset code user entered
     */
    public boolean verifyResetCode(String email, String code) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByUserAndCode(user, code)
            .orElse(null);

        // Check if token exists
        if (resetToken == null) {
            return false;
        }

        // Check if expired
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Check if already used
        if (resetToken.isUsed()) {
            return false;
        }

        return true;
    }

    /**
     * Step 3: Reset the password after code is verified
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
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

        // Send confirmation email
        emailService.sendPasswordChangedConfirmation(user.getEmail(), user.getFirstName());
        
        log.info("Password reset successfully for user: {}", email);
    }

    /**
     * Resend code if user didn't receive it
     */
    @Transactional
    public void resendResetCode(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        // Delete old tokens
        passwordResetTokenRepository.deleteByUser(user);
        
        // Start fresh password reset
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
    }
}