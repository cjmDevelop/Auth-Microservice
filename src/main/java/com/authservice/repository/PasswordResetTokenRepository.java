package com.authservice.repository;

import com.authservice.model.PasswordResetToken;
import com.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find reset token by user and code
     */
    Optional<PasswordResetToken> findByUserAndCode(User user, String code);

    /**
     * Find the most recent valid token for a user
     */
    Optional<PasswordResetToken> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    /**
     * Delete all tokens for a specific user
     */
    void deleteByUser(User user);

    /**
     * Delete expired tokens (for cleanup)
     */
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    /**
     * Check if a valid token exists for user
     */
    boolean existsByUserAndUsedFalseAndExpiryDateAfter(User user, LocalDateTime dateTime);
}