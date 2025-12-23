package com.authservice.service;

import com.authservice.dto.auth.AccountDeletionDto;
import com.authservice.model.AppSource;
import com.authservice.model.User;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account Deletion Service
 *
 * Handles permanent account deletion with:
 * - Password verification for security
 * - Complete data cleanup via cascade delete (notes, tokens, quiz results, etc.)
 * - User completely removed from database (allows email reuse for new signups)
 * - Confirmation email
 * - GDPR compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailService emailService;

    /**
     * Delete user account permanently (hard delete)
     *
     * Steps:
     * 1. Verify password for security
     * 2. Save user info for confirmation email
     * 3. Log deletion reason if provided
     * 4. Permanently delete user (cascade delete removes all related data)
     * 5. Send confirmation email
     *
     * @param request Contains email, password, and optional reason
     * @throws IllegalArgumentException if password is incorrect or user not found
     */
    @Transactional
    public void deleteAccount(AccountDeletionDto request) {
        log.info("🗑️ Account deletion requested for: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify password for security
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("❌ Invalid password for account deletion: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid password");
        }

        // Save user info before deletion (needed for email)
        String userName = user.getFirstName();
        String userEmail = user.getEmail();
        AppSource appSource = user.getAppSource();
        Long userId = user.getId();

        // Optional: Log deletion reason for analytics (before deletion)
        if (request.getReason() != null && !request.getReason().isBlank()) {
            log.info("📊 Deletion reason for {}: {}", userEmail, request.getReason());
        }

        // HARD DELETE: Permanently remove user and all related data
        // Cascade delete will automatically remove:
        // - All notes
        // - All verification tokens
        // - All password reset tokens
        // - All quiz results
        userRepository.delete(user);
        log.info("✅ User account permanently deleted: {} (ID: {})", userEmail, userId);

        // Send confirmation email (async)
        emailService.sendAccountDeletionConfirmation(userEmail, userName, appSource);
        log.info("📧 Account deletion confirmation email queued for: {}", userEmail);
    }

    /**
     * Check if user can delete their account
     * (Optional: add business rules here)
     *
     * @param email User's email
     * @return true if account can be deleted
     */
    public boolean canDeleteAccount(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Add any business rules here
        // For example: check if user has pending payments, etc.

        return true; // For now, all users can delete their accounts
    }
}
