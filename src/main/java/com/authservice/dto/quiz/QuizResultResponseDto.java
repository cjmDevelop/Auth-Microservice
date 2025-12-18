package com.authservice.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for quiz result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponseDto {

    private Long id;
    private String quizDomain;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Double percentage;
    private Boolean passed;
    private Integer timeTakenSeconds;
    private Boolean isTimed;
    private LocalDateTime completedAt;
}
