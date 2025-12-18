package com.authservice.repository;

import com.authservice.model.QuizResult;
import com.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * QuizResultRepository - Data access layer for quiz results
 */
@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    /**
     * Find all quiz results for a specific user
     */
    List<QuizResult> findByUserOrderByCompletedAtDesc(User user);

    /**
     * Find all quiz results for a specific user and quiz domain
     */
    List<QuizResult> findByUserAndQuizDomainOrderByCompletedAtDesc(User user, String quizDomain);

    /**
     * Find the best (highest score) result for a user and quiz domain
     */
    @Query("SELECT qr FROM QuizResult qr WHERE qr.user = :user AND qr.quizDomain = :quizDomain ORDER BY qr.percentage DESC")
    Optional<QuizResult> findBestResultByUserAndQuizDomain(@Param("user") User user, @Param("quizDomain") String quizDomain);

    /**
     * Find the most recent result for a user and quiz domain
     */
    Optional<QuizResult> findFirstByUserAndQuizDomainOrderByCompletedAtDesc(User user, String quizDomain);

    /**
     * Count total quiz attempts for a user
     */
    long countByUser(User user);

    /**
     * Count passed quizzes for a user
     */
    long countByUserAndPassedTrue(User user);
}
