package com.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * QuizResult entity - Tracks user quiz attempts and scores
 * Stores quiz results for progress tracking and analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quiz_results")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quiz_domain", nullable = false)
    private String quizDomain; // e.g., "1.1", "1.2", "2.1", etc.

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "percentage", nullable = false)
    private Double percentage;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "is_timed")
    @Builder.Default
    private Boolean isTimed = false;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
        // Calculate percentage
        if (totalQuestions != null && correctAnswers != null && totalQuestions > 0) {
            percentage = (correctAnswers * 100.0) / totalQuestions;
        }
        // Determine if passed (70% or higher)
        if (percentage != null) {
            passed = percentage >= 70.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalculate on update
        if (totalQuestions != null && correctAnswers != null && totalQuestions > 0) {
            percentage = (correctAnswers * 100.0) / totalQuestions;
        }
        if (percentage != null) {
            passed = percentage >= 70.0;
        }
    }
}
