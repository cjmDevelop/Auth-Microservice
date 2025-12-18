package com.authservice.controller;

import com.authservice.dto.quiz.QuizResultRequestDto;
import com.authservice.dto.quiz.QuizResultResponseDto;
import com.authservice.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * QuizResultController - REST endpoints for quiz result management
 */
@RestController
@RequestMapping("/api/quiz-results")
@RequiredArgsConstructor
@Slf4j
public class QuizResultController {

    private final QuizResultService quizResultService;

    /**
     * Submit a new quiz result
     * POST /api/quiz-results
     */
    @PostMapping
    public ResponseEntity<?> submitQuizResult(
            @RequestBody QuizResultRequestDto request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            log.info("Quiz result submission from user: {}", email);

            QuizResultResponseDto result = quizResultService.saveQuizResult(email, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Quiz result saved successfully",
                    "result", result
            ));

        } catch (Exception e) {
            log.error("Error saving quiz result", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all quiz results for the authenticated user
     * GET /api/quiz-results
     */
    @GetMapping
    public ResponseEntity<?> getUserQuizResults(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<QuizResultResponseDto> results = quizResultService.getUserQuizResults(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", results
            ));

        } catch (Exception e) {
            log.error("Error retrieving quiz results", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get quiz results for a specific domain
     * GET /api/quiz-results/domain/{domain}
     */
    @GetMapping("/domain/{domain}")
    public ResponseEntity<?> getQuizResultsByDomain(
            @PathVariable String domain,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            List<QuizResultResponseDto> results = quizResultService
                    .getUserQuizResultsByDomain(email, domain);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", results
            ));

        } catch (Exception e) {
            log.error("Error retrieving quiz results for domain: {}", domain, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get the best result for a specific domain
     * GET /api/quiz-results/domain/{domain}/best
     */
    @GetMapping("/domain/{domain}/best")
    public ResponseEntity<?> getBestResultByDomain(
            @PathVariable String domain,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            QuizResultResponseDto result = quizResultService.getBestResultByDomain(email, domain);

            if (result == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "result", (Object) null,
                        "message", "No results found for this quiz"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "result", result
            ));

        } catch (Exception e) {
            log.error("Error retrieving best result for domain: {}", domain, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get user quiz statistics
     * GET /api/quiz-results/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getUserStatistics(Authentication authentication) {
        try {
            String email = authentication.getName();
            QuizResultService.QuizStatisticsDto stats = quizResultService.getUserStatistics(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", stats
            ));

        } catch (Exception e) {
            log.error("Error retrieving user statistics", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
