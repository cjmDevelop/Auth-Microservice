package com.authservice.service;

import com.authservice.dto.quiz.QuizResultRequestDto;
import com.authservice.dto.quiz.QuizResultResponseDto;
import com.authservice.model.QuizResult;
import com.authservice.model.User;
import com.authservice.repository.QuizResultRepository;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * QuizResultService - Business logic for quiz result management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizResultService {

    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;

    /**
     * Save a new quiz result
     */
    @Transactional
    public QuizResultResponseDto saveQuizResult(String email, QuizResultRequestDto request) {
        log.info("Saving quiz result for user: {} - Quiz: {}", email, request.getQuizDomain());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QuizResult quizResult = QuizResult.builder()
                .user(user)
                .quizDomain(request.getQuizDomain())
                .totalQuestions(request.getTotalQuestions())
                .correctAnswers(request.getCorrectAnswers())
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .isTimed(request.getIsTimed() != null ? request.getIsTimed() : false)
                .build();

        QuizResult saved = quizResultRepository.save(quizResult);

        log.info("Quiz result saved - User: {}, Quiz: {}, Score: {}/{} ({}%), Passed: {}",
                email, saved.getQuizDomain(), saved.getCorrectAnswers(), saved.getTotalQuestions(),
                String.format("%.1f", saved.getPercentage()), saved.getPassed());

        return convertToDto(saved);
    }

    /**
     * Get all quiz results for a user
     */
    public List<QuizResultResponseDto> getUserQuizResults(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<QuizResult> results = quizResultRepository.findByUserOrderByCompletedAtDesc(user);

        log.info("Retrieved {} quiz results for user: {}", results.size(), email);

        return results.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get quiz results for a specific quiz domain
     */
    public List<QuizResultResponseDto> getUserQuizResultsByDomain(String email, String quizDomain) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<QuizResult> results = quizResultRepository
                .findByUserAndQuizDomainOrderByCompletedAtDesc(user, quizDomain);

        log.info("Retrieved {} results for user: {} - Quiz: {}", results.size(), email, quizDomain);

        return results.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get the best result for a specific quiz domain
     */
    public QuizResultResponseDto getBestResultByDomain(String email, String quizDomain) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return quizResultRepository.findBestResultByUserAndQuizDomain(user, quizDomain)
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * Get user quiz statistics
     */
    public QuizStatisticsDto getUserStatistics(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long totalAttempts = quizResultRepository.countByUser(user);
        long passedCount = quizResultRepository.countByUserAndPassedTrue(user);

        return QuizStatisticsDto.builder()
                .totalAttempts(totalAttempts)
                .passedCount(passedCount)
                .failedCount(totalAttempts - passedCount)
                .build();
    }

    /**
     * Convert QuizResult entity to DTO
     */
    private QuizResultResponseDto convertToDto(QuizResult result) {
        return QuizResultResponseDto.builder()
                .id(result.getId())
                .quizDomain(result.getQuizDomain())
                .totalQuestions(result.getTotalQuestions())
                .correctAnswers(result.getCorrectAnswers())
                .percentage(result.getPercentage())
                .passed(result.getPassed())
                .timeTakenSeconds(result.getTimeTakenSeconds())
                .isTimed(result.getIsTimed())
                .completedAt(result.getCompletedAt())
                .build();
    }

    /**
     * DTO for quiz statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QuizStatisticsDto {
        private Long totalAttempts;
        private Long passedCount;
        private Long failedCount;
    }
}
