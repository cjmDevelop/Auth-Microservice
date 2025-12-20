package com.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.authservice.model.User;

/**
 * UserRepository.java
 * Repository for User database operations
 * JPA generates SQL based on method name.
 * Spring Data JPA dependency handles CRUD operations without manual implementation
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    // App-specific queries
    Optional<User> findByEmailAndAppSource(String email, com.authservice.model.AppSource appSource);
    Optional<User> findByPhoneNumberAndAppSource(String phoneNumber, com.authservice.model.AppSource appSource);
} 