package com.authservice.service;

import com.authservice.dto.auth.AccountDeletionDto;
import com.authservice.model.User;
import com.authservice.repository.NoteRepository;
import com.authservice.repository.PasswordResetTokenRepository;
import com.authservice.repository.UserRepository;
import com.authservice.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Account Deletion Service
 *
 * Handles soft account deletion with:
 * - Password verification for security
 * - Complete data cleanup (notes, tokens, etc.)
 * - User marked as deleted (allows email reuse)
 * - Confirmation email
 * - GDPR compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailService emailService;

    /**
     * Delete user account (soft delete)
     *
     * Steps:
     * 1. Verify password for security
     * 2. Delete all user's notes
     * 3. Delete all verification tokens
     * 4. Delete all password reset tokens
     * 5. Mark user account as deleted (allows email reuse)
     * 6. Send confirmation email
     *
     * @param request Contains email, password, and optional reason
     * @throws IllegalArgumentException if password is incorrect
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

        String userName = user.getFirstName();
        String userEmail = user.getEmail();

        // Step 1: Delete all user's notes
        int notesDeleted = noteRepository.findByUserId(user.getId()).size();
        noteRepository.deleteAll(noteRepository.findByUserId(user.getId()));
        log.info("📝 Deleted {} notes for user: {}", notesDeleted, userEmail);

        // Step 2: Delete all verification tokens
        verificationTokenRepository.deleteAll(
            verificationTokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .toList()
        );
        log.info("🔑 Deleted verification tokens for user: {}", userEmail);

        // Step 3: Delete all password reset tokens
        passwordResetTokenRepository.deleteByUser(user);
        log.info("🔐 Deleted password reset tokens for user: {}", userEmail);

        // Step 4: Soft delete user account (mark as deleted, allows email reuse)
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);
        userRepository.save(user);
        log.info("✅ User account marked as deleted: {}", userEmail);

        // Step 5: Send confirmation email (async)
        emailService.sendAccountDeletionConfirmation(userEmail, userName, user.getAppSource());
        log.info("📧 Account deletion confirmation email queued for: {}", userEmail);

        // Optional: Log deletion reason for analytics
        if (request.getReason() != null && !request.getReason().isBlank()) {
            log.info("📊 Deletion reason: {}", request.getReason());
        }
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
