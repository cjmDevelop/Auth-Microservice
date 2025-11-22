package com.authservice.repository;

import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.authservice.model.VerificationToken;
import com.authservice.model.User;

/**
 * VerificationTokenRepository.java 
 * Repository for verification token database operations.
 * JPA generates SQL based on method name.
 * Spring Data JPA dependency handles CRUD operations without manual implementation
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByCodeAndUserAndType(
        String code,
        User user,
        VerificationToken.VerificationType type
    );

    Optional<VerificationToken> findByUserAndTypeAndVerifiedAtIsNull(
        User user,
        VerificationToken.VerificationType type
    );
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
